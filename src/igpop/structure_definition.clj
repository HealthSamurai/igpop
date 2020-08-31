(ns igpop.structure-definition
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [flatland.ordered.map :refer [ordered-map]])
  (:import [java.net URLEncoder]
           [java.util.zip ZipOutputStream ZipEntry]))

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
    :disabled (when value {:max 0})
    :minItems {:min value}
    :maxItems {:max value}))

(defmethod prop->sd :constant
  [_ id _ _ value]
  (->> (str/split id #"\.")
       last
       capitalize
       (str "fixed")
       keyword
       (conj (list value))
       (apply assoc {})))

(defn- convert-constraint [[k rules]]
  (let [d (:description rules)
        s (:severity rules "error")
        props (select-keys rules [:requirements :expression :xpath :source])
        props (assoc props :severity s)
        props (if d (assoc props :human d) props)]
    (into (ordered-map :key (name k)) props)))

(defmethod prop->sd :constraints
  [_ _ _ _ value]
  (->> value
       (mapv convert-constraint)
       (assoc {} :constraint)))

(defmethod prop->sd :union
  [_ id path _ value]
  ;TODO consider porting the second implementation from the original
  {:id (str id "[x]")
   :path (str path "[x]")
   :type (mapv (fn [t] {:code t}) value)})

(defmethod prop->sd :refers
  [_ _ _ _ value]
  (->> value
       (filter :resourceType)
       (map (juxt :resourceType :profile))
       (map (partial map url-encode))
       ;TODO take the base URL from the project ctx
       (map (partial apply format "https://healthsamurai.github.io/igpop/profiles/%s/%s.html"))
       (map vector)
       (mapv (partial assoc (ordered-map {:code "Reference"}) :targetProfile))
       (assoc {} :type)))

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

(defn path->id [path]
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
  (let [id (path->id path)
        path (path->str path)
        result (ordered-map
                {:id id
                 :path path
                 :mustSupport true})]
    (->> element
         (map (juxt key val))
         (map (partial apply prop->sd element id path))
         (reduce into result))))

(defmulti flatten-element
  "Turn nested structure into flat map with keys representing path in the original structure."
  {:arglists '([path element])}
  (fn [path _] (last path)))

(defmethod flatten-element :default
  [path element]
  (->> (get element :elements)
       (map (juxt (comp (partial conj path) key) val))
       (map (partial apply flatten-element))
       (into {path (dissoc element :elements)})))

(defmethod flatten-element :extension
  [path element]
  (->> element
       (map (juxt (comp (partial conj path) key)
                  (comp #(dissoc % :elements)
                        #(assoc % :type [{:code "Extension"}])
                        val)))
       (map (partial apply flatten-element))
       (into {})))

(defn convert [type element]
  (->> (flatten-element [type] element)
       (map (juxt key val))
       (mapv (partial apply element->sd))))

(defn extension->structure-definition
  "Transforms IgPop extension to a structure definition."
  [prefix context-type id diff snapshot]
  {:resourceType "StructureDefinition"
   :id (str/join "-" [prefix (name context-type) (name id)])
   :description (or (:description diff) (:description snapshot))
   :type "Extension"
   :context [{:type "element", :expression (name context-type)}]
   :snapshot {:element (convert :Extension (if (:elements snapshot) snapshot {:elements {:value snapshot}}))}
   :differential {:element (convert :Extension (if (:elements diff) diff {:elements {:value diff}}))}})

(defn profile->structure-definition
  "Transforms IgPop profile to a structure definition."
  [prefix type id diff snapshot]
  {:resourceType "StructureDefinition"
   :id (str prefix "-" (name type) (when (not= :basic id) (str "-" (name id))))
   :description (or (:description diff) (:description snapshot))
   :type (name type)
   :snapshot {:element (convert type snapshot)}
   :differential {:element (convert type diff)}})

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
  "Transforms IgPop profile into a set of structure definitions."
  [prefix type id diff snapshot]
  (let [profile (profile->structure-definition prefix type id diff snapshot)
        extensions (->> (get-extensions diff)
                        (map (juxt (comp last key) val (comp (partial get-in snapshot) key)))
                        (map (partial apply extension->structure-definition prefix type)))]
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
        vsets (->> valuesets
                   (map (partial ig-vs->valueset (:id ctx)))
                   (map (fn [x] {:fullUrl (str (:base-url ctx) "/" (:id x))
                                 :resource x})))
        profiles (->> diff-profiles
                      (mapcat (fn [[type prls]] (for [[id diff] prls] [type id diff (get-in snapshot [type id])])))
                      (mapcat (partial apply ig-profile->structure-definitions (:id ctx)))
                      (map (fn [sd]
                             {:fullUrl  (str (:base-url ctx) "/" (:id sd))
                              :resource sd})))
        result {:resourceType "Bundle"
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
         :type "fihr.ig"
         :date (java.util.Date.)
         :canonical (:url ctx)
         :fihrVersions [(:fhir ctx)]))))

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
