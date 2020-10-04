(ns igpop.structure-definition
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [flatland.ordered.map :refer [ordered-map]])
  (:import [java.net URLEncoder]
           [java.util.zip ZipOutputStream ZipEntry]))


(def igpop-properties
  "IgPop special properties wich will be interpreted
  and discarded from Structure Defineitison structures"
  #{:disabled :required :minItems :maxItems
    :elements :union :constant :constraints
    :valueset :description :collection})

(defn- url-encode [s] (URLEncoder/encode s "UTF-8"))

(defn- take-while+
  [pred coll]
  (lazy-seq
   (when-let [[f & r] (seq coll)]
     (if (pred f)
       (cons f (take-while+ pred r))
       [f]))))

(defn- capitalize
  "Uppercase first character of the string.
  Unlike `clojure.string/capitalize` - we don't lowercase rest of the string"
  [^String s]
  (str (.toUpperCase (subs s 0 1)) (subs s 1)))

(def prop-hierarchy
  (-> (make-hierarchy)
      (derive :disabled ::cardinality)
      (derive :required ::cardinality)
      (derive :minItems ::cardinality)
      (derive :maxItems ::cardinality)))

(defmulti prop->sd
  "Convert an element's property to Structure Definition property."
  {:arglists '([element id path prop value])}
  (fn [_ _ _ prop _] prop)
  :hierarchy #'prop-hierarchy)

(defmethod prop->sd :default [_ _ _ prop value] {prop value})

(defmethod prop->sd ::cardinality
  [_ _ _ prop value]
  (condp = prop
    :required (when value {:min 1})
    :disabled (when value {:max "0"})
    :minItems {:min value}
    :maxItems {:max (str value)}))

(defmethod prop->sd :constant
  [_ id _ _ value]
  {(->> (str/split id #"\.") last capitalize (str "fixed") keyword)
   value})

(defn- convert-constraint [[k rules]]
  (let [d (:description rules)
        s (:severity rules "error")
        props (select-keys rules [:requirements :expression :xpath :source])
        props (assoc props :severity s)
        props (if d (assoc props :human d) props)]
    (into (ordered-map :key (name k)) props)))

(defmethod prop->sd :constraints
  [_ _ _ _ value]
  {:constraint (mapv convert-constraint value)}
  #_(->> value
         (mapv convert-constraint)
         (assoc {} :constraint)))

(defmethod prop->sd :union
  [_ id path _ value]
  ;TODO consider porting the second implementation from the original
  {:id (str id "[x]")
   :path (str path "[x]")
   :type (mapv (fn [t] {:code t}) value)})

;; TODO take the base URL from the project ctx
(defmethod prop->sd :refers
  [_ _ _ _ value]
  (letfn [(make-url [{rt :resourceType p :profile}]
            (format "https://healthsamurai.github.io/igpop/profiles/%s/%s.html"
                    (url-encode rt) (url-encode p)))]
    {:type (mapv #(ordered-map {:code "Reference" :targetProfile [(make-url %)]})
                 (filter :resourceType value))}))

(defmethod prop->sd :valueset
  [_ _ _ _ value]
  {:binding {:valueSet (str "https://healthsamurai.github.io/igpop/valuesets/" (:id value) ".html")
             :strength (:strength value "extensible")
             :description (:description value)}})

(defmethod prop->sd :mappings
  [_ _ _ _ value]
  {:mapping
   (mapv
    (fn [[k v]] (into (ordered-map {:identity (name k)}) v))
    value)})

(defmethod prop->sd :description
  [_ _ _ _ value]
  {:short value})

(defmethod prop->sd :type
  [element _ _ _ value]
  (if (or (contains? element :union)
          (and (vector? value) (map? (first value))))
    {:type value}
    {:type [{:code value}]}))

(defn path->id
  "Convert path (vector) to joined string with special separator"
  [path]
  (->> path
       (mapcat #(if (= :extension %) [% ":"] [% "."]))
       drop-last
       (map name)
       (apply str)))

(defn path->str [path]
  (->> path
       (take-while+ (partial not= :extension))
       (map name)
       (str/join ".")))

(defn element->sd
  "Convert an igpop element to its Structure Definition representation."
  [path element]
  (let [id     (path->id path)
        path   (path->str path)
        result (ordered-map {:id id :path path :mustSupport true})]
    (->> element
         (mapv (fn [[prop value]]
                 (prop->sd element id path prop value)))
         (reduce into result))))

(defmulti flatten-element
  "Turn nested structure into flat map with keys
  representing path in the original structure.

  path    - vector of keys
  element - ig-pop element"
  {:arglists '([path element])}
  (fn [path _] (last path)))

(defmethod flatten-element :default
  [path element]
  (->> (get element :elements)
       (map #(flatten-element (conj path (key %))
                              (val %)))
       (apply merge (ordered-map {path (dissoc element :elements)}))))


(defmethod flatten-element :extension
  [path element]
  (apply merge
         (ordered-map {path {:path (str/join "." (map name path))
                             :slicing {:discriminator [{:type "value" :path "url"}]
                                       :ordered false
                                       :rules "open"}}})
         (map (fn [[id el]]
                (flatten-element
                 (conj path id)
                 (-> el
                     (dissoc :elements)
                     (dissoc :url)
                     (assoc :sliceName (name id)
                            :isModifier false
                            :type [{:code "Extension"
                                    :profile [(format "https://healthsamurai.github.io/ig-ae/profiles/%s/%s"
                                                      (url-encode (name (first path)))
                                                      (url-encode (name id)))]}]))))
              element)))

(defn convert
  "Convert `element` (recurcive struct)
  with `type` to flattened StructureDefinition for resource"
  [type element]
  (->> (flatten-element [type] element)
       (rest)  ;;  remove root element from definition
       (mapv (fn [[path el]]
               (element->sd path el)))))

(defn convert-ext
  "Convert `element` (recurcive struct)
  with `type` to flattened StructureDefinition for extension"
  [type element]
  (->> (flatten-element [type] element)
       (mapv (fn [[path el]]
               (-> (element->sd path el))))))

(defn extension->structure-definition
  "Transforms IgPop extension to a structure definition.

  project-id    - string, that will be used as prefix of all urls
  profile-type  - keyword for resource type
  profile-id    - profile-id for profile-type. (used as postfix in urls)
  diff          - differential profile
  snapshot      - snapshoted profile
  "
  [project-id profile-type profile-id diff snapshot]
  {:resourceType   "StructureDefinition"
   :id             (str/join "-" [project-id (name profile-type) (name profile-id)])
   :name           (name profile-id) ;; REVIEW - is this correct value?
   :description    (or (:description diff) (:description snapshot))
   :status         "active"
   :fhirVersion    "4.0.1" ;; TODO get from ctx
   :kind           "complex-type"
   :abstract       false
   :type           "Extension"
   :url            (:url diff)
   :baseDefinition "http://hl7.org/fhir/StructureDefinition/Extension"
   :derivation     "constraint"
   :context        [{:type "element", :expression (name profile-type)}]
   ;; :snapshot {:element (convert :Extension (if (:elements snapshot) snapshot {:elements {:value snapshot}}))}
   :differential {:element (convert :Extension (if (:elements diff) diff {:elements {:value diff}}))}})

(defn profile->structure-definition
  "Transforms IgPop profile to a structure definition.

  project-id    - string, that will be used as prefix of all urls
  profile-type  - keyword for resource type
  profile-id    - profile-id for profile-type. (used as postfix in urls)
  diff          - differential profile
  snapshot      - snapshoted profile
  "
  [project-id profile-type profile-id diff snapshot]
  (merge {:resourceType "StructureDefinition"
          :id           (str project-id "-" (name profile-type) (when (not= :basic profile-id) (str "-" (name profile-id))))
          :description  (or (:description diff) (:description snapshot))
          :type         (name profile-type)
          :name         (when (not= :basic profile-id) (name profile-id))
          :status       "active"
          :fhirVersion  "4.0.1"
          :abstract     false
          }
         ;; url: "https://healthsamurai.github.io/ig-ae/profiles/StructureDefinition/AZAdverseEvent"
         ;; name: "az-adverseevent"
         ;; baseDefinition: "http://hl7.org/fhir/StructureDefinition/AdverseEvent"
         ;; derivation: constraint
         ;; title: ''
         ;; context: {}
         ;; :kind        "complex-type"
         ;; identifier: {}
         ;; version: ''
         ;; description: This is the AZ profile (StructureDefinition) for AdverseEvent
         ;; elements: {}
         (apply dissoc diff igpop-properties) ;; <---- TODO: replace this with explicit field enumeration
         {;; :snapshot {:element (convert profile-type snapshot)}
          :differential {:element (convert profile-type diff)}}))

(defn get-extensions
  "Returns a flattened map of nested extensions where key is a path in
   the original structure and val is an extension."
  ([x] (get-extensions [] x))
  ([path x]
   (let [p (conj path :elements)
         pe (conj p :extension)
         nested (->> (dissoc (:elements x) :extension)
                     (map (juxt (comp (partial conj p) key) val))
                     (mapcat (partial apply get-extensions)))]
     (->> (get-in x [:elements :extension])
          (map (juxt (comp (partial conj pe) key) val))
          (mapcat (fn [[k v]] (cons [k v] (get-extensions k v))))
          (concat nested)
          (into {})))))

(defn ig-profile->structure-definitions
  "Transforms IgPop profile into a set of structure definitions.

  project-id    - string, that will be used as prefix of all urls
  profile-type  - keyword for resource type
  profile-id    - profile-id for profile-type. (used as postfix in urls)
  diff          - differential profile
  snapshot      - snapshoted profile
  "
  [project-id profile-type profile-id diff snapshot]
  (let [profile (profile->structure-definition project-id profile-type profile-id diff snapshot)
        extensions (map (fn [[path element-diff]]
                          (extension->structure-definition
                           project-id profile-type (last path) element-diff (get-in snapshot path)))
                        (get-extensions diff))]
    (->> (cons profile extensions)
         (filter some?)
         (into []))))

(defn ig-vs->valueset
  "Trnsforms IgPop valueset to a canonical valuest."
  [prefix [id body]]
  {:resourceType "ValueSet"
   :id (str prefix "-" (name id))
   :name (->> (str/split (name id) #"-") (map capitalize) (apply str))
   :title (str/replace (name id) "-" " ")
   :status (:status body "active")
   :date (:date body (java.util.Date.))
   :compose {:include {:concept (:concepts body)}}})

(defn project->bundle
  "Transforms IgPop project to a bundle of structure definitions."
  [ctx]
  (let [{:keys [valuesets diff-profiles snapshot]} ctx
        vsets    (for [ig-vs valuesets
                       :let [vset (ig-vs->valueset (:id ctx) ig-vs)]]
                   {:fullUrl (str (:base-url ctx) "/" (:id vset))
                    :resource vset})
        profiles (for [[type profiles-by-id] diff-profiles
                       [id diff] profiles-by-id
                       struct-def (ig-profile->structure-definitions (:id ctx) type id diff (get-in snapshot [type id]))]
                   {:fullUrl  (str (:base-url ctx) "/" (:id struct-def))
                    :resource struct-def})
        result   {:resourceType "Bundle"
                  :id "resources"
                  :meta {:lastUpdated (java.util.Date.)}
                  :type "collection"}]
    (->> (concat vsets profiles)
         (into [])
         (assoc result :entry))))

(defmulti generate-package!
  "Generate a Structure Definitions package of specified type.
   Returns the path to generated package."
  {:arglists '([type ig-ctx & {:as opts}])}
  (fn [type & _] type))

(defn npm-manifest [ctx]
  (let [manifest (io/file (:home ctx) "package.json")
        data (json/parse-stream (io/reader manifest) true)]
    (-> data
        (merge (select-keys ctx [:version :license :title :author :url]))
        (assoc
         :type "fhir.ig"
         :date (java.util.Date.)
         :canonical (:url ctx)
         :fhirVersions [(:fhir ctx)]))))

;; Generate a set of JSON Structure Definition files and zip them up.
;; Returns zip `File`.
(defmethod generate-package! :npm
  [_ ig-ctx & {:as opts}]
  (let [bundle (project->bundle ig-ctx)
        resources (map :resource (:entry bundle))
        manifest (npm-manifest ig-ctx)
        file (or (:file opts) (io/file (:home ig-ctx) "build" (str (:name manifest) ".zip")))]
    (io/make-parents file)
    (with-open [output (ZipOutputStream. (io/output-stream file))
                writer (io/writer output)]
      (doseq [resource resources]
        (let [entry-name (str (:resourceType resource) "/" (:id resource) ".json")
              entry (ZipEntry. entry-name)]
          (.putNextEntry output entry)
          (json/generate-stream resource writer)
          (.closeEntry output)))
      (.putNextEntry output (ZipEntry. "package.json"))
      (json/generate-stream manifest writer)
      (.closeEntry output))
    file))


(comment
  (def test-base (clj-yaml.core/parse-string (slurp "./npm/fhir-4.0.0/src/AdverseEvent.yaml")))
  (def test-profile (clj-yaml.core/parse-string (slurp "../ig-ae/src/AdverseEvent.yaml")))
  (def bad-result-data (json/parse-string (slurp "my_tasks/hl7.fhir.ae-AdverseEvent-AZEmployeeReporter.json")))

  (extension->structure-definition [] "" "id" test-data test-data)



  ; -------------------------------------

  (def hm (.getAbsolutePath (io/file  "../ig-ae")))

  (require '[igpop.loader])

  (def ctx (igpop.loader/load-project hm))

  (defn dump [x & [format]]
    (println "dump")
    (spit "./my_tasks/temp.yaml"
          (if (= format :json)
            (cheshire.core/generate-string x {:pretty true})
            (clj-yaml.core/generate-string x))))

  (dump (-> ctx :diff-profiles :AdverseEvent))
  (dump (-> ctx :resources :AdverseEvent))
  (dump (-> ctx :profiles :AdverseEvent))

  (dump
   (ig-profile->structure-definitions
    "prefix"
    :AZAdverseEvent
    :AZAdverseEvent
    test-profile
    test-base
    ;; (:diff-profile ctx)
    ;; (:snapshot ctx)
    )
   )

  (dump (project->bundle ctx)
        :json)

  (clojure.pprint/pprint (-> ctx :base :profiles))

  (flatten-element [ :AZAdverseEvent ] test-profile)

  (project->bundle ctx)


  (generate-package! :npm ctx :file (temp-file "package" ".zip"))

  -------------------------------------)
