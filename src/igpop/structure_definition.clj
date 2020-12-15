(ns igpop.structure-definition
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [flatland.ordered.map :refer [ordered-map]])
  (:import [java.net URLEncoder]
           [java.util.zip ZipOutputStream ZipEntry]))


(def resource-root-keys
  "Resource root keys, specified in natural order."
  [:resourceType :id :url :identifier :version :name :title :status
   :experimental :date :publisher :contact :description :useContext :jurisdiction
   :purpose :copyright :keyword :fhirVersion :mapping :kind :abstract :context
   :contextInvariant :type :baseDefinition :derivation :snapshot :differential])

;; TODO: delete this value (we use resouce-root-keys for filtering purpose).
;; Or maybe we can use these keys in special-keys analysers (future ideas)
(def igpop-properties
  "IgPop special properties wich will be interpreted
  and discarded from Structure Defineitison structures"
  #{:disabled :required :minItems :maxItems
    :elements :union :constant :constraints
    :valueset :description :collection})

(defn- url-encode [s] (URLEncoder/encode s "UTF-8"))

(defn- format-url
  "Make url from 'url-template' with special tokens '%s'
  Replaces tokens with url-encoded parts"
  [url-template & parts]
  (apply format url-template (map url-encode parts)))

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

;; -------------------------------- URL utils -------------------------------

(defn url? [o]
  (and (string? o) (str/starts-with? o "http")))

(defn make-profile-id [project-id profile-type profile-id]
  (str project-id "-" (name profile-type)
       (when (not= :basic profile-id)
         (str "-" (name profile-id)))))


(defn make-profile-url [manifest profile-type profile-id]
  (format "%s/StructureDefinition/%s-%s%s"
          (:url manifest) (:id manifest) (name profile-type)
          (if (= (name profile-id) "basic") "" (str "-" (name profile-id)))))


;; (defn make-extension-url [base-url ext-url]
;;   (when ext-url
;;     (if (url? ext-url)
;;       ext-url
;;       (str base-url "/" ext-url))))

(defn make-extension-url [manifest profile-type extension-id]
  (format "%s/StructureDefinition/%s-%s%s"
          (:url manifest) (:id manifest) (name profile-type)
          (if (= (name extension-id) "basic") "" (str "-" (name extension-id)))))


(defn make-valueset-url [manifest value-id]
  (format "%s/ValueSet/%s-%s"
          (:url manifest) (:id manifest)  (name value-id)))



;; (format-url (str (:url manifest) "/StructureDefinition/%s-%s")
;;             (name (first path)) (name (peek path)))


;; -------------------------------- prop->sd -------------------------------

(def prop-hierarchy
  (-> (make-hierarchy)
      (derive :collection ::cardinality)
      (derive :disabled ::cardinality)
      (derive :required ::cardinality)
      (derive :minItems ::cardinality)
      (derive :maxItems ::cardinality)))

(defmulti prop->sd
  "Convert an element's property to Structure Definition property."
  {:arglists '([manifest element id path prop value])}
  (fn [_ _ _ _ prop _] prop)
  :hierarchy #'prop-hierarchy)

(defmethod prop->sd :default [_ _ _ _ prop value] {prop value})

(defmethod prop->sd ::cardinality
  [_ _ _ _ prop value]
  (condp = prop
    :collection (when value {:min 0 :max "*"})
    :required   (when value {:min 1})
    :disabled   (when value {:max "0"})
    :minItems   {:min value}
    :maxItems   {:max (str value)}))

(defmethod prop->sd :constant
  [_ _ id _ _ value]
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
  [_ _ _ _ _ value]
  {:constraint (mapv convert-constraint value)}
  #_(->> value
         (mapv convert-constraint)
         (assoc {} :constraint)))

(defmethod prop->sd :union
  [_ _ id path _ value]
  ;TODO consider porting the second implementation from the original
  {:id (str id "[x]")
   :path (str path "[x]")
   :type (mapv (fn [t] {:code t}) value)})

(defmethod prop->sd :refers
  [manifest _ _ _ _ value]
  (letfn [(make-url [{rt :resourceType p :profile}]
            (make-profile-url manifest rt p)
            #_(format-url (str (:url manifest) "/profiles/%s/%s") rt p))]
    {:type (mapv #(ordered-map {:code "Reference" :targetProfile [(make-url %)]})
                 (filter :resourceType value))}))

(defmethod prop->sd :valueset
  [manifest _ _ _ _ value]
  {:binding {:valueSet (make-valueset-url manifest (:id value))
             :strength (:strength value "extensible")
             :description (:description value)}})

(defmethod prop->sd :mappings
  [_ _ _ _ _ value]
  {:mapping
   (mapv
    (fn [[k v]] (into (ordered-map {:identity (name k)}) v))
    value)})

(defmethod prop->sd :description
  [_ _ _ _ _ value]
  {:short value})

(defmethod prop->sd :type
  [_ element _ _ _ value]
  (if (or (contains? element :union)
          (and (vector? value) (map? (first value))))
    {:type value}
    {:type [{:code value}]}))

;; FIXME Dumb and naive solution. Find better alternative.
(defn url->profile-name
  "Asuume that url is in one of the formats:
    - http://somewhere.org/fhir/uv/myig/hl7.fhir.ae-HumanName
    - http://somewhere.org/fhir/uv/myig/hl7.fhir.ae.HumanName
    - http://somewhere.org/fhir/uv/myig/hl7/fhir/ae/HumanName
  We try extract 'HumanName' from this"
  [url]
  (when url
    (->> (cond (str/includes? url "-") "-"
               (str/includes? url ".") "."
               (str/includes? url "/") "/")
         (str/last-index-of url)
         inc
         (subs url))))


(defmethod prop->sd :profile
  [manifest _ _ _ _ value]
  {:type [(if (url? value)
            {:code (url->profile-name value) :profile [value]}
            (let [[rt profile-id]
                  (if (not (str/includes? value "-")) [value :basic]
                      (str/split value #"-"))                   ;; NOTE: assume that we have only ONE dash
                  base (get-in manifest [:diff-profiles (keyword rt) (keyword profile-id) :baseDefinition])] ;; <-- FIXME: Leaky abstraction.. Try find another way to get `base`
              {:code (if (url? base) (-> (str/split base #"/") last) base)
               :profile [(make-extension-url manifest rt profile-id)]}))]})


;; ----------------------------- PATH utils ---------------------------------

(defn path-extension-root?
  "Is path determine an extension root
  Something like [... _ _ :extension]"
  [path]
  (= :extension (peek path)))

(defn path-extension?
  "Is path determine a named extension
  Something like [... _ _ :extension _]"
  [path]
  (and (> (count path) 1)
       (not= :extension (peek path))
       (= :extension (-> path pop peek))))

(defn path-extension-element?
  "Is path determine a nested element of named extension
  Something like [... :extension _ :elements _]"
  [path]
  (let [n (count path)]
    (and (> n 3)
         (not= :extension (nth path (- n 1)))
         (= :elements (nth path (- n 2)))
         (not= :extension (nth path (- n 3)))
         (= :extension (nth path (- n 4))))))

(defn path-nested-extension?
  "Is path determine a nested extension (extension in extension)
  Something like [... :extension _ :elements _ :extension _ ] "
  [path]
  (and (> (count path) 2)
       (> (count (filter #{:extension} path)) 1)))

(defn path->sd-path
  "Remove ':elements' part from path"
  [path] (into [] (remove #{:elements}) path))


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

;; ----------------------------- PROFILE utils ---------------------------------

(defn element->sd
  "Convert an igpop element to its Structure Definition representation."
  [manifest [path element]]
  (let [id     (path->id path)
        path   (path->str path)
        result (ordered-map :id id :path path :mustSupport true)]
    (->> element
         (mapv (fn [[prop value]]
                 (prop->sd manifest element id path prop value)))
         (reduce into result))))


(defn choose-next-steps
  "Choose next steps according to path.
  Returns vector of `[current-element next-elements next-path]`"
  [path element]
  (cond (path-extension-root? path)
        [element element path]
        :default
        [(dissoc element :elements) (get element :elements) (conj path :elements)]))

(defn reduce-profile
  "Walk on profile and call 'f' on every element (or extension)
  * 'f' - function like (fn [init path element]) => init'
  * 'init' - accumulation value,
  * 'path' - position of 'element' in the profile tree.
  (NOTE: We try to preserve walk order)"
  ([f init element] (reduce-profile f init [] element))
  ([f init path element]
   (let [[cur-el next-els next-path] (choose-next-steps path element)
         init' (f init path cur-el)
         make-step (fn [acc id el] (reduce-profile f acc (conj next-path id) el))]
     (reduce-kv make-step init' next-els))))


(def default-first-extension-element
  "Element that will be placed before extension elements"
  {:slicing {:discriminator [{:type "value" :path "url"}]
             :ordered false
             :rules "open"}})

(defn enrich-extension-element-with-slice-name [manifest path el]
  (-> (dissoc el :elements :url)
      (assoc :sliceName (name (peek path))
             :isModifier false
             :type [{:code "Extension"
                     :profile [(format-url (str (:url manifest) "/StructureDefinition/%s-%s")
                                           (name (first path)) (name (peek path)))]}])))


(defn flatten-element
  "Turn nested structure into flat map with keys
  representing path in the original structure.
  path    - vector of keys
  element - ig-pop element"
  [manifest path element]
  (reduce-profile
   (fn [acc path el]
     (cond
       (or (path-nested-extension? path)
           (path-extension-element? path))
       acc  ;; ignore for now

       (path-extension-root? path)
       (assoc acc (path->sd-path path)
              default-first-extension-element)

       (path-extension? path)
       (assoc acc (path->sd-path path)
              (enrich-extension-element-with-slice-name manifest path el))

       :else
       (assoc acc (path->sd-path path) el)))
   (ordered-map)
   path element))

(defn convert-profile-elements
  "Convert `element` (recurcive struct)
  with `type` to flattened StructureDefinition for resource"
  [manifest type element]
  (->> (flatten-element manifest [type] element)
       (rest)  ;;  remove root element from definition
       (mapv (partial element->sd manifest))))


(defn convert-nested-extension-elements
  "Convert `element` (recurcive struct)
  with `type` to flattened StructureDefinition for extension"
  [manifest type element]
  (->> (flatten-element manifest [type] element)
       (map (fn [[k v]] [k (dissoc v :url)]))
       (mapv (partial element->sd manifest))))

(defn simple-flatten-element
  "Like flatten-element, but does not interpret extension as special case.
  We need in Extension SD element with id : Extension.extension."
  [path element]
  (apply merge
         (ordered-map path (dissoc element :elements))
         (for [[id el] (get element :elements)]
           (simple-flatten-element (conj path id) el))))

;; (defn simple-flatten-element2
;;   "Like flatten-element, but does not interpret extension as special case.
;;   We need in Extension SD element with id : Extension.extension."
;;   [path element]
;;   (reduce-profile assoc (ordered-map) path element))


;; HACK: bad staff here - our structure is not correct profile.
(defn enrich-simple-extension
  "When we met simple(not nested) extension property - we need to generate
  additional elements for it in Extension SD file"
  [manifest fixedUri diff]
  (merge
   {:elements {:extension {:minItems 0 :maxItems 0}
               :url {:minItems 1 :maxItems 1 :fixedUri fixedUri}
               "value[x]" {:minItems 1 :maxItems 1 :type (:type diff)}}}
   (select-keys diff [:minItems :maxItems])))


(defn convert-simple-extension-elements
  "Convert `element`with `type` to sequenced StructureDefinition for simple extension"
  [manifest fixedUri type element]
  (->>
   ;; (assoc element :url (make-extension-url (:url manifest) (:url element)))
   (assoc element :url fixedUri)
   (enrich-simple-extension manifest fixedUri)
   (simple-flatten-element [type])
   (map (fn [[k v]] [k (dissoc v :url)]))
   (mapv (partial element->sd manifest))))

;; (enrich-simple-extension {:minItems 10})

(def resource-root-keys-sort-order
  (zipmap resource-root-keys (range)))

(defn resource-keys-comparator [x y]
  (compare (resource-root-keys-sort-order x)
           (resource-root-keys-sort-order y)))

(defn extension->structure-definition
  "Transforms IgPop extension to a structure definition.

  manifest      - igpop manifest map
  profile-type  - keyword for resource type
  profile-id    - profile-id for profile-type. (used as postfix in urls)
  diff          - differential profile
  snapshot      - snapshoted profile
  "
  [manifest profile-type profile-id diff snapshot]
  (merge
   (sorted-map-by resource-keys-comparator)
   ;; -- default values
   {:name           (name profile-id) ;; REVIEW - is this correct value?
    :description    (or (:description diff) (:description snapshot))
    :status         "active"
    :fhirVersion    (:fhir manifest)
    :kind           "complex-type" ;; always a complex-type
    :abstract       false
    :baseDefinition "http://hl7.org/fhir/StructureDefinition/Extension"
    :derivation     "constraint"
    :context        [{:type "element", :expression (name profile-type)}]}
   ;; -- replaced by user values
   (select-keys diff resource-root-keys)
   ;; -- pinned values
   {:resourceType "StructureDefinition"
    :type         "Extension"
    ;; :id           (str/join "-" [(:id manifest) (name profile-type) (name profile-id)])
    :id           (make-profile-id (:id manifest) profile-type profile-id)
    ;; :url          (make-extension-url (:url manifest) (:url diff))
    :url          (make-extension-url manifest profile-type profile-id)
    ;; :snapshot {:element (convert-ext :Extension (if (:elements snapshot) snapshot {:elements {:value snapshot}}))}
    :differential {:element
                   (if (:elements diff)
                     (convert-nested-extension-elements manifest :Extension diff)
                     ;; HACK for id = "Extension"  min/max should came from diff.
                     ;;  for id = "Extension.value" min/max = 1 - by default (At least for now) - [Vitaly 06.10.2020]
                     (convert-simple-extension-elements manifest
                                                        (make-extension-url manifest profile-type profile-id)
                                                        :Extension diff))}}))


(defn profile->structure-definition
  "Transforms IgPop profile to a structure definition.

  manifest      - igpop manifest map
  profile-type  - keyword for resource type
  profile-id    - profile-id for profile-type. (used as postfix in urls)
  diff          - differential profile
  snapshot      - snapshoted profile
  "
  [manifest profile-type profile-id diff snapshot]
  (merge
   (sorted-map-by resource-keys-comparator)
   ;; -- default values
   {
    :description  (or (:description diff) (:description snapshot))
    :type         (name profile-type)
    :name         (when (not= :basic profile-id) (name profile-id))
    :kind         "resource" ;; resource or complex-type
    :status       "active"
    :fhirVersion  (:fhir manifest)
    :abstract     false}
   ;; url: "https://healthsamurai.github.io/ig-ae/profiles/StructureDefinition/AZAdverseEvent"
   ;; name: "az-adverseevent"
   ;; :derivation constraint
   ;; title: ''
   ;; context: {}
   ;; identifier: {}
   ;; version: ''
   ;; description: This is the AZ profile (StructureDefinition) for AdverseEvent
   ;; elements: {}
   ;; -- replaced by user values
   (select-keys diff resource-root-keys)
   ;; -- pinned values
   {:resourceType "StructureDefinition"
    :id           (make-profile-id (:id manifest) profile-type profile-id)
    :url          (make-profile-url manifest profile-type profile-id)
    ;; -- :snapshot {:element (convert-profile-elements manifest profile-type snapshot)}
    :differential {:element (convert-profile-elements manifest profile-type diff)}}))


(defn get-extensions
  "Returns a flattened map of nested extensions where key is a path in
   the original structure and val is an extension."
  ([x] (get-extensions [] x))
  ([path x]
   (reduce-profile (fn [acc path el]
                     (cond-> acc (path-extension? path) (assoc path el)))
                   (ordered-map) path x)))


(defn ig-profile->structure-definitions
  "Transforms IgPop profile into a set of structure definitions.

  manifest      - igpop manifest map
  profile-type  - keyword for resource type
  profile-id    - profile-id for profile-type. (used as postfix in urls)
  diff          - differential profile
  snapshot      - snapshoted profile
  "
  [manifest profile-type profile-id diff snapshot]
  (let [profile (profile->structure-definition manifest profile-type profile-id diff snapshot)
        extensions (->> (get-extensions diff)
                        ;; (remove #{:profile}) ;; NOTE: Remove extensions wich refers to profiles. We don't need to generate SD files for them
                        (map (fn [[path element-diff]]
                               (extension->structure-definition
                                manifest profile-type (last path) element-diff (get-in snapshot path)))))]
    (->> (cons profile extensions)
         (filter some?)
         (into []))))

(defn ig-vs->valueset
  "Trnsforms IgPop valueset to a canonical valuest."
  [manifest [id body]]
  {:resourceType "ValueSet"
   :id (str (:id manifest) "-" (name id))
   :name (->> (str/split (name id) #"-") (map capitalize) (apply str))
   :title (str/replace (name id) "-" " ")
   :status (:status body "active")
   :date (:date body (java.util.Date.))
   :compose {:include [(merge (select-keys body [:system])
                              {:concept (:concepts body)})]}})

(defn project->bundle
  "Transforms IgPop project to a bundle of structure definitions."
  [ctx]
  (let [{:keys [valuesets diff-profiles snapshot]} ctx
        vsets    (for [ig-vs valuesets
                       :let [vset (ig-vs->valueset ctx ig-vs)]]
                   {:fullUrl (str (:base-url ctx) "/" (:id vset))
                    :resource vset})
        profiles (for [[type profiles-by-id] diff-profiles
                       [id diff] profiles-by-id
                       struct-def (ig-profile->structure-definitions ctx type id diff (get-in snapshot [type id]))]
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

  (require 'clj-yaml.core)
  (def test-base (clj-yaml.core/parse-string (slurp "./npm/fhir-4.0.0/src/AdverseEvent.yaml")))
  (def test-profile (clj-yaml.core/parse-string (slurp "../ig-ae/src/AdverseEvent.yaml")))
  (def bad-result-data (json/parse-string (slurp "my_tasks/hl7.fhir.ae-AdverseEvent-AZEmployeeReporter.json")))

  (extension->structure-definition [] "" "id" test-profile test-profile)



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
  (dump (-> ctx :diff-profiles keys))
  (dump (-> ctx :resources :AdverseEvent))
  (dump (-> ctx :profiles :Patient))
  (-> ctx :profiles :Practitioner :basic :baseDefinition)
  (-> ctx :profiles :Practitioner :basic)

  (prop->sd ctx {} :Practitioner [] :profile "HumanName")

  (dump (-> ctx :diff-profiles :Address))
  (dump
   (first
    (ig-profile->structure-definitions
     ctx
     ;; {:url "http://my-super-site"
     ;;  :id "ig-az"
     ;;  :BAD "GGG"}
     :Address
     :basic
                                        ;test-profile
     ;; (-> ctx :profiles :Address :basic)
     ;; (-> ctx :profiles :Address :basic)
                                        ;test-base
     ;; (:diff-profile ctx)
     ;; (:snapshot ctx)
     )
    )
   )


  (-> ctx :profiles :HumanName)

  (dump (profile->structure-definition
         {:url "prefix"} :AZAdverseEvent :AZAdverseEvent test-profile test-base))

  (dump (project->bundle ctx)
        :json)

  (clojure.pprint/pprint (-> ctx :base :profiles))

  ;; (flatten-element [ :AZAdverseEvent ] test-profile)
  (let [ext (val (first (get-extensions test-profile)))]
    (convert-ext {} :Extension
                 (merge {:elements {:value (assoc ext :minItems 0 :maxItems 0)}}
                        (select-keys ext [:minItems :maxItems]))))

  (project->bundle ctx)


  (generate-package! :npm ctx :file (temp-file "package" ".zip"))


  -------------------------------------)
