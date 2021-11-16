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

(def valueset-resource-root-keys
  [:resourceType :id :url :identifier :version :name :title :status :experimental :date :publisher
   :contact :description :useContext :jurisdiction :immutable :purpose :copyright
   :compose :expansion])

(def codesystem-resource-root-keys
  [:resourceType :id :url :identifier :version :name :title :status :experimental
   :date :publisher :contact :description :useContext :jurisdiction :purpose
   :copyright :caseSensitive :valueSet :hierarchyMeaning :compositional
   :versionNeeded :content :supplements :count :filter :property :concept])

(def restricted-keys-in-elements
  "Keys that cannot be in differential.elements"
  (disj (set resource-root-keys) :type :short))

;; TODO: delete this value (we use resouce-root-keys for filtering purpose).
;; Or maybe we can use these keys in special-keys analysers (future ideas)
(def igpop-properties
  "IgPop special properties wich will be interpreted and discarded from Structure Defineitison structures"
  #{:disabled :required :minItems :maxItems
    :elements :union :constant :constraints
    :valueset :description :collection})

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

(defn make-profile-id [prefix profile-type profile-id]
  (str (or prefix "") (name profile-type)
       (when (not= :basic profile-id)
         (str "-" (name profile-id)))))

(defn make-profile-url [manifest profile-type profile-id]
  (format "%s/StructureDefinition/%s%s%s"
          (:url manifest) (:prefix manifest "") (name profile-type)
          (if (= (name profile-id) "basic") "" (str "-" (name profile-id)))))

(defn make-extension-url [manifest profile-type extension-id]
  (format "%s/StructureDefinition/%s%s%s"
          (:url manifest) (:prefix manifest "") (name profile-type)
          (if (= (name extension-id) "basic") "" (str "-" (name extension-id)))))

(defn make-valueset-url [manifest value-id]
  (format "%s/ValueSet/%s%s"
          (:url manifest) (:prefix manifest "") (name (or value-id "MISSED_VS_ID"))))

(defn make-codesystem-url [manifest value-id]
  (format "%s/CodeSystem/%s%s"
          (:url manifest) (:prefix manifest "") (name value-id)))


(defn make-valueset-id [prefix value-id]
  (str (or prefix "") (name value-id)))

(defn make-valueset-title [valueset-id]
  (str/replace (name valueset-id) "-" " "))

(defn make-valueset-name [valueset-id]
  (->> (str/split (name valueset-id) #"-")
       (map capitalize)
       (apply str)))

(defn make-codesystem-id [prefix system-id]
  (make-valueset-id prefix system-id))

(defn make-codesystem-title [codesystem-id]
  (make-valueset-title codesystem-id))

(defn make-codesystem-name [codesystem-id]
  (make-valueset-name codesystem-id))


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
  {:constraint (mapv convert-constraint value)})

(defmethod prop->sd :union
  [_ _ id path _ value]
  ;TODO consider porting the second implementation from the original
  {:id (str id "[x]")
   :path (str path "[x]")
   :type (mapv (fn [t] {:code t}) value)})

(defmethod prop->sd :refers
  [manifest _ _ _ _ value]
  (letfn [(make-url [{rt :resourceType p :profile}]
            (make-profile-url manifest rt p))]
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
       (map (fn [x] (when x (name x) (pr-str "ERROR:" path))))
       (apply str)))

(defn path->str [path]
  (->> path
       (take-while+ (partial not= :extension))
       (map name)
       (str/join ".")))

(defn sd-path->context-expression
  "Return context.expression of extension by it's path (sd-path)"
  [path] (str/join "." (map name (remove #{:basic :extension} (butlast path)))))


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
        :else
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
                     :profile [(make-extension-url manifest (name (first path)) (name (peek path)))]}])))


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
  with `type` to flattened StructureDefinition for resource
  Also cleanup all root properties from nested elements"
  [manifest type element]
  (->> (flatten-element manifest [type] element)
       (rest)  ;;  remove root element from definition
       (map (fn [[k v]] [k (apply dissoc v restricted-keys-in-elements)]))
       (mapv (partial element->sd manifest))))


(defn convert-nested-extension-elements
  "Convert `element` (recurcive struct)
  with `type` to flattened StructureDefinition for extension"
  [manifest type element]
  (->> (flatten-element manifest [type] element)
       (map (fn [[k v]] [k (dissoc v :url)]))
       (map (fn [[k v]] [k (apply dissoc v restricted-keys-in-elements)]))
       (mapv (partial element->sd manifest))))

(defn simple-flatten-element
  "Like flatten-element, but does not interpret extension as special case.
  We need in Extension SD element with id : Extension.extension."
  [path element]
  (apply merge
         (ordered-map path (dissoc element :elements))
         (for [[id el] (get element :elements)]
           (simple-flatten-element (conj path id) el))))

;; HACK: bad staff here - our structure is not correct profile.
(defn enrich-simple-extension
  "When we met simple(not nested) extension property - we need to generate
  additional elements for it in Extension SD file"
  [_manifest fixedUri diff]
  (merge
   {:elements {:extension {:minItems 0 :maxItems 0}
               :url {:minItems 1 :maxItems 1 :fixedUri fixedUri}
               "value[x]" {:minItems 1 :maxItems 1 :type (:type diff)}}}
   (select-keys diff [:minItems :maxItems])))


(defn convert-simple-extension-elements
  "Convert `element`with `type` to sequenced StructureDefinition for simple extension"
  [manifest fixedUri type element]
  (->>
   (assoc element :url fixedUri)
   (enrich-simple-extension manifest fixedUri)
   (simple-flatten-element [type])
   (map (fn [[k v]] [k (dissoc v :url)]))
   (mapv (partial element->sd manifest))))

(def resource-root-keys-sort-order
  (zipmap resource-root-keys (range)))

(def valueset-resource-root-keys-sort-order
  (zipmap valueset-resource-root-keys (range)))

(def codesystem-resource-root-keys-sort-order
  (zipmap codesystem-resource-root-keys (range)))

(defn resource-keys-comparator [x y]
  (compare (resource-root-keys-sort-order x)
           (resource-root-keys-sort-order y)))

(defn valueset-resource-keys-comparator [x y]
  (compare (valueset-resource-root-keys-sort-order x)
           (valueset-resource-root-keys-sort-order y)))

(defn codesystem-resource-keys-comparator [x y]
  (compare (codesystem-resource-root-keys-sort-order x)
           (codesystem-resource-root-keys-sort-order y)))


(defn extension->structure-definition
  "Transforms IgPop extension to a structure definition.

  manifest       - igpop manifest map
  resource-type   - keyword for resource type
  extension-path - path of extension in profile
  diff           - differential profile
  snapshot       - snapshoted profile
  "
  [manifest resource-type extension-path diff snapshot]
  (let [extension-id (last extension-path)]
    (merge
     (sorted-map-by resource-keys-comparator)
   ;; -- default values
     {:name           (name extension-id) ;; REVIEW - is this correct value?
      :description    (or (:description diff) (:description snapshot))
      :status         "active"
      :fhirVersion    (:fhir manifest)
      :kind           "complex-type" ;; always a complex-type
      :abstract       false
      :baseDefinition "http://hl7.org/fhir/StructureDefinition/Extension"
      :derivation     "constraint"
      :context        [{:type "element",
                        :expression (sd-path->context-expression extension-path)}]}
   ;; -- replaced by manifest values
     (select-keys manifest [:publisher :date])
   ;; -- replaced by user values
     (select-keys diff resource-root-keys)
   ;; -- pinned values
     {:resourceType "StructureDefinition"
      :type         "Extension"
      :id           (make-profile-id (:prefix manifest) resource-type extension-id)
      :url          (make-extension-url manifest resource-type extension-id)
    ;; :snapshot {:element (convert-ext :Extension (if (:elements snapshot) snapshot {:elements {:value snapshot}}))}
      :differential {:element
                     (if (:elements diff)
                       (convert-nested-extension-elements manifest :Extension diff)
                     ;; HACK for id = "Extension"  min/max should came from diff.
                     ;;  for id = "Extension.value" min/max = 1 - by default (At least for now) - [Vitaly 06.10.2020]
                       (convert-simple-extension-elements manifest
                                                          (make-extension-url manifest resource-type extension-id)
                                                          :Extension diff))}})))

(defn sd-extension? [resource]
  (and (= "StructureDefinition" (:resourceType resource))
       (= "Extension" (:type resource))))

(defn profile->structure-definition
  "Transforms IgPop profile to a structure definition.

  manifest      - igpop manifest map
  resource-type - keyword for resource type
  profile-id    - profile-id for resource-type.
  diff          - differential profile
  snapshot      - snapshoted profile
  "
  [manifest resource-type profile-id diff snapshot]
  (merge
   (sorted-map-by resource-keys-comparator)
   ;; -- default values
   {
    :description  (or (:description diff) (:description snapshot))
    :type         (name resource-type)
    :name         (when (not= :basic profile-id) (name profile-id))
    :kind         "resource" ;; resource or complex-type
    :status       "active"
    :fhirVersion  (:fhir manifest)
    :abstract     false}
   ;; url: "https://healthsamurai.github.io/ig-ae/profiles/StructureDefinition/AZAdverseEvent"
   ;; :derivation constraint
   ;; title: ''
   ;; context: {}
   ;; identifier: {}
   ;; version: ''
   ;; -- replaced by manifest values
   (select-keys manifest [:publisher :date])
   ;; -- replaced by user values
   (select-keys diff resource-root-keys)
   ;; -- pinned values
   {:resourceType "StructureDefinition"
    :id           (make-profile-id (:prefix manifest) resource-type profile-id)
    :url          (make-profile-url manifest resource-type profile-id)
    ;; -- :snapshot {:element (convert-profile-elements manifest resource-type snapshot)}
    :differential {:element (convert-profile-elements manifest resource-type diff)}}))

(defn sd-profile? [resource]
  (and (= "StructureDefinition" (:resourceType resource))
       (not= "Extension" (:type resource))))

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
  resource-type  - keyword for resource type
  profile-id    - profile-id for resource-type. (used as postfix in urls)
  diff          - differential profile
  snapshot      - snapshoted profile
  "
  [manifest resource-type profile-id diff snapshot]
  (let [profile (profile->structure-definition manifest resource-type profile-id diff snapshot)
        extensions (->> (get-extensions diff)
                        ;; (remove #{:profile}) ;; NOTE: Remove extensions wich refers to profiles. We don't need to generate SD files for them
                        (map (fn [[path element-diff]]
                               (extension->structure-definition
                                manifest resource-type (path->sd-path path) element-diff (get-in snapshot path)))))]
    (->> (cons profile extensions)
         (filter some?)
         (into []))))

(defn ig-vs->valueset
  "Transform IgPop valueset to a canonical valuest."
  [manifest [id body]]
  (merge
   (sorted-map-by valueset-resource-keys-comparator)
   ;; get from manifest
   (select-keys manifest [:publisher]) ;; :date injected below
   {:resourceType "ValueSet"
    :id      (make-valueset-id (:prefix manifest) id)
    :url     (make-valueset-url manifest id)
    :name    (make-valueset-name id)
    :title   (make-valueset-title id)
    :status  (:status body "active")
    :date    (or (:date body) (:date manifest) (java.util.Date.))
    :compose {:include [(merge (select-keys body [:system])
                               {:concept (:concepts body)})]}}))

(defn sd-valueset? [resource]
  (= "ValueSet" (:resourceType resource)))

(defn ig-cs->codesystem
  "Transform IgPop CodeSystem to a canonical CodeSystem."
  [manifest [id body]]
  (merge
   (sorted-map-by codesystem-resource-keys-comparator)
   ;; get from manifest
   (select-keys manifest [:publisher]) ;; :date injected below
   ;; default values
   {:resourceType "CodeSystem"
    :name    (make-codesystem-name id)
    :title   (make-codesystem-title id)
    :date    (or (:date body) (:date manifest) (java.util.Date.))
    :status  "active"
    :content "complete"}
   ;; -- replaced by user values
   (select-keys body codesystem-resource-root-keys)
   ;; -- pinned values
   {:id  (make-codesystem-id (:prefix manifest) id)
    :url (make-codesystem-url manifest id)
    :concept (:concepts body)}))

(defn sd-codesystem? [resource]
  (= "CodeSystem" (:resourceType resource)))

(defn project->structure-definitions
  "Generate structure-definitions (profiles/valuesets/extensions) from project context"
  [{:keys [valuesets codesystems diff-profiles snapshot] :as ctx}]
  (let [vsets    (for [ig-vs valuesets]
                   (ig-vs->valueset ctx ig-vs))
        csys     (for [ig-cs codesystems]
                   (ig-cs->codesystem ctx ig-cs))
        profiles (for [[type profiles-by-id] diff-profiles
                       [id diff] profiles-by-id
                       struct-def (ig-profile->structure-definitions ctx type id diff (get-in snapshot [type id]))]
                   struct-def)]
    {:valuesets   vsets
     :codesystems csys
     :structure-definitions profiles}))

(defn ctx-build-structure-definitions [ctx]
  (let [{:keys [valuesets codesystems
                structure-definitions]} (project->structure-definitions ctx)]
    (assoc ctx :generated
           {:valuesets   (zipmap (map :id valuesets) valuesets)
            :codesystems (zipmap (map :id codesystems) codesystems)
            :structure-definitions (zipmap (map :id structure-definitions) structure-definitions)})))

(defn project->bundle
  "Transforms IgPop project to a bundle of structure definitions."
  [ctx]
  {:resourceType "Bundle"
   :id "resources"
   :meta {:lastUpdated (java.util.Date.)}
   :type "collection"
   :entry (vec
           (for [res (mapcat val (project->structure-definitions ctx))]
             {:fullUrl  (str (:base-url ctx) "/" (:id res))
              :resource res}))})


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
      (->> resources
           (reduce (fn [idx resource]
                     (let [entry-name (str (:resourceType resource) "/" (:id resource) ".json")
                           entry (ZipEntry. entry-name)]
                       (if-not (get idx entry-name)
                         (do
                           (.putNextEntry output entry)
                           (json/generate-stream resource writer)
                           (.closeEntry output)
                           (assoc idx entry-name true))
                         (do 
                           (println "Duplicate entry: " entry-name)
                           idx)))) {}))
      (.putNextEntry output (ZipEntry. "package.json"))
      (json/generate-stream manifest writer)
      (.closeEntry output))
    file))


(comment

  (require 'clj-yaml.core)
  (def test-base (clj-yaml.core/parse-string (slurp "./npm/fhir-4.0.0/src/AdverseEvent.yaml")))
  (def test-profile (clj-yaml.core/parse-string (slurp "../ig-ae/src/AdverseEvent.yaml")))


  ; -------------------------------------

  (def hm (.getAbsolutePath (io/file  "../ig-ae")))

  (require '[igpop.loader])

  (def ctx (igpop.loader/load-project hm))

  (defn dump [x & [format]]
    (spit "./my_tasks/temp.yaml"
          (if (= format :json)
            (cheshire.core/generate-string x {:pretty true})
            (clj-yaml.core/generate-string x))))


  (-> ctx :path :Address)
  (dump (-> ctx :diff-profiles :Address))
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
     :Observation
     :basic
                                        ;test-profile
     (-> ctx :profiles :Observation)
     (-> ctx :profiles :Observation)
                                        ;test-base
     ;; (:diff-profile ctx)
     ;; (:snapshot ctx)
     )
    )
   )

  (dump (get-in ctx [:profiles :Observation :basic :elements :extension :pregnancyFlag]))
  (dump (get-in ctx [:diff-profiles :Observation :basic :elements :extension :pregnancyFlag]))
  (dump (get-in ctx [:profiles :Observation :basic :elements :extension :pregnancyFlag]))
  (dump (get-in ctx [:profiles :Observation :basic :elements :extension :pregnancyFlag]))

  (-> ctx :valuesets keys)

  (dump (profile->structure-definition
         {:url "prefix"} :AZAdverseEvent :AZAdverseEvent test-profile test-base))

  (dump (project->bundle ctx)
        :json)

  (clojure.pprint/pprint (-> ctx :base :profiles))

  ;; (flatten-element [ :AZAdverseEvent ] test-profile)
  (let [ext (val (first (get-extensions test-profile)))]
    #_(convert-ext {} :Extension
                 (merge {:elements {:value (assoc ext :minItems 0 :maxItems 0)}}
                        (select-keys ext [:minItems :maxItems]))))

  (project->bundle ctx)


  (generate-package! :npm ctx :file #_(temp-file "package" ".zip"))


 ;; -------------------------------------
  )
