(ns igpop.structure-definition
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [flatland.ordered.map :refer [ordered-map]])
  (:import [java.util.zip ZipEntry ZipOutputStream]))

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

(defn- name-that-profile
  [type id]
  (if (= :basic id)
    (name type)
    (str (name type) "_" (name id))))

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
  {:hierarchy #'prop-hierarchy})

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

(defmethod prop->sd :constraints
  [_ _ _ _ value]
  (->> value
       ;(mapv ) ;TODO needs implementation
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
       (filter (comp (partial = :resourceType) key))
       (map val)
       (map #(str "https://healthsamurai.github.io/igpop/profiles/" % "/basic.html"))
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
       (mapcat #(if (= :Extension %) [% ":"] [% "."]))
       drop-last
       (map name)
       str))

(defn path->str [path]
  (->> path
       (take-while+ (partial not= :Extension))
       (map name)
       (str/join ".")))

(defn el->sd [path element]
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

(defmethod flatten-element :Extension
  [path element]
  (->> element
       (map (juxt (comp (partial conj path) key) val))
       (map (partial apply flatten-element))
       (into {})))

(defn convert [type element]
  (->> (flatten-element [type] element)
       (map (juxt key val))
       (mapv (partial apply el->sd))))

(defn profile->structure-definition
  "Transforms IgPop profile to a structure definition."
  [type id diff snapshot]
  {:resourceType "StructureDefinition"
   :id (name id)
   :description (or (:description diff) (:description snapshot))
   :type (name type)
   :snapshot {:element (convert type snapshot)}
   :differential {:element (convert type diff)}})

(defn project->bundle
  "Transforms IgPop project to a bundle of structure definitions."
  [ctx]
  (let [{:keys [valuesets diff-profiles snapshot]} ctx
        result {:resourceType "Bundle"
                :id "resources"
                :meta {:lastUpdated (java.util.Date.)}
                :type "collection"}]
    (->> diff-profiles
         (mapcat (fn [[type prls]] (for [[id diff] prls] [type id diff])))
         (mapv (fn [[type id diff]]
                 {:fullUrl  (str "baseUrl" "/" (name-that-profile type id)) ;TODO infer baseUrl from the context
                  :resource (profile->structure-definition type id diff (get-in snapshot [type id]))}))
         (assoc result :entry))))

(defmulti generate-package!
  "Generate a Structure Definitions package of specified type.
   Returns the path to generated package."
  {:arglists '([type ig-ctx & [opts]])}
  (fn [type & _] type))

(defn node-js-package-meta [ctx]
  (assoc
   (select-keys ctx [:version :license :title :author :url])
   :name (:id ctx)
   :type "fihr.ig"
   :date "";ISO timestamp
   :canonical (:url ctx)
   :fihrVersions [(:fhir ctx)]
   :dependencies {};lib to version
   :directories {};name to path
   ))

;; Generate a set of JSON Structure Definition files and zip them up.
;; Returns zip `File`.
(defmethod generate-package! :node-js
  [_ ig-ctx & [opts]]
  (let [bundle (project->bundle ig-ctx)
        resources (map :resource (:entry bundle))
        file (or (:file opts) (io/file (:home ig-ctx) "build" "package.zip"))]
    (io/make-parents file)
    (with-open [output (ZipOutputStream. (io/output-stream file))
                writer (io/writer output)]
      (doseq [resource resources]
        (let [entry-name (str (:type resource) "-" (:id resource) ".json")
              entry (ZipEntry. entry-name)]
          (.putNextEntry output entry)
          (json/generate-stream resource writer)
          (.closeEntry output)))
      (.putNextEntry output (ZipEntry. "package.json"))
      (json/generate-stream (node-js-package-meta ig-ctx) writer)
      (.closeEntry output))
    file))
