(ns igpop.profile.to-json-rule
  (:require
    [flatland.ordered.map :refer :all]
    [clojure.string :as s]
    [clojure.pprint :as p]
    ))


(def constraint-struct '(:requirements :severity :human :expression :xpath :source))

(defn fhirpath-rule
  [k coll]
  {k (mapv
       (fn [item]
         (let [k (first (keys item))
               v (first (vals item))]
           (reduce
             (fn [acc k]
               (into acc
                     (if (contains? v k)
                       {k (get v k)}
                       (cond
                         (= k :severity) {k "error"}
                         (= k :human) (if-let [str (get v :description)] {k str})))))
             (ordered-map {:key (name k)}) constraint-struct)))
       coll)})

(defn mustSupport
  [profile-id k v]
  {:mustSupport v})

(defn cardinality [profile-id k v] (cond
                                     (= k :required) (if (= true v) {:min 1})
                                     (= k :disabled) (if (= true v) {:max 0})
                                     (= k :minItems) {:min v}
                                     (= k :maxItems) {:max v}))

;;target url = https://healthsamurai.github.io/igpop/profiles/{resourceType}/basic.html
(defn refers [profile-id k v]
  {:type
   (reduce (fn [outer-acc ordmap]
             (conj outer-acc
                   (reduce (fn [acc [key val]]
                             (into acc (if (= key :resourceType)
                                         {:targetProfile [(str "https://healthsamurai.github.io/igpop/profiles/" val "/basic.html")]})))
                           (ordered-map {:code "Reference"}) ordmap)
                   )) [] v)})

(defn valueset
  [profile-id k v]
  (let [resource (last (s/split "fhir:administrative-gender" #":"))
        value-set-url (str "http://hl7.org/fhir/ValueSet/" resource)]
    {:binding {
               :strength "required",
               :valueSet value-set-url
               }}))

(defn description [profile-id k v])

(defn collection [profile-id k v])

(defn val-type [profile-id k v]
  {:type [{:code v}]})

(defn poly [profile-id k v])

(defn fixedUri [profile-id type val]
  {:fixedUri (name val)})

(defn slice [profile-id type val]
  {:sliceName (name val)})

(defn extension [profile-id key value]
  (slice profile-id "Extension" value))

(defn extension-reference
  [profile-id key value]
  (let [url (str "http://hl7.org/fhir/us/core/StructureDefinition/" (name profile-id) "-" value)]
    {:type [{:code    "Extension",
             :profile [url]}]} ))

(def agenda {:required            cardinality
             :disabled            cardinality
             :minItems            cardinality
             :maxItems            cardinality
             :constraints         fhirpath-rule
             :mustSupport         mustSupport
             :refers              refers
             :valueset            valueset
             :description         description
             :collection          collection
             :type                val-type
             :union               poly
             :value               poly
             :extension           extension
             :sliceName           slice
             :fixedUri            fixedUri
             :extension-reference extension-reference})

(defn ->str-path
  [vec-path]
  (clojure.string/join "." (map name (remove nil? vec-path))))