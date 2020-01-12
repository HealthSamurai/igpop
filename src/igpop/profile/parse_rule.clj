(ns igpop.profile.parse-rule
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
  ([] (mustSupport true))
  ([v] {:mustSupport v}))

(defn cardinality [k v] (cond
                          (= k :required) (if (= true v) {:min 1})
                          (= k :disabled) (if (= true v) {:max 0})
                          (= k :minItems) {:min v}
                          (= k :maxItems) {:max v}))

;;target url = https://healthsamurai.github.io/igpop/profiles/{resourceType}/basic.html
(defn refers [k v]
  {:type
   (reduce (fn [outer-acc ordmap]
             (conj outer-acc
                   (reduce (fn [acc [key val]]
                             (into acc (if (= key :resourceType)
                                         {:targetProfile [(str "https://healthsamurai.github.io/igpop/profiles/" val "/basic.html")]})))
                           (ordered-map {:code "Reference"}) ordmap)
                   )) [] v)
   }
  )

(defn valueset
  [k v]
  (let [resource (last (s/split "fhir:administrative-gender" #":"))
        value-set-url (str "http://hl7.org/fhir/ValueSet/" resource)]
    {:binding {
               :strength "required",
               :valueSet value-set-url
               }}))

(defn description [k v])

(defn collection [k v])

(defn val-type [k v]
  )

(defn poly [k v]
  ;(ordered-map {:type (map (fn [entry] {:code entry}) v)})
  )

(defn parse-extension
  [extension]

  )

(defn extension [k v])

(def agenda {:required    cardinality
             :disabled    cardinality
             :minItems    cardinality
             :maxItems    cardinality
             :constraints fhirpath-rule
             :mustSupport mustSupport
             :refers      refers
             :valueset    valueset
             :description description
             :collection  collection
             :type        val-type
             :union       poly
             :value       poly
             :extension   extension})

(def default-agenda {:mustSupport mustSupport})
