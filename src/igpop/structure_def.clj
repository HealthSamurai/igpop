(ns igpop.structure-def
  (:require [flatland.ordered.map :refer [ordered-map]]
            [clojure.string :as str]))

(defn name-that-profile
  [rt prn]
  (str (name rt) (when (not (= "basic" (name prn)))
                   (str "_" (name prn)))))

(defn element-processing
  [acc el parent-name]
  {:id (str parent-name "." (name el))})

(defn get-path
  [prefix key]
  (str prefix "." (name key)))

(defn flatten-extension
  [path ext]
  (->> ext
       (map (juxt (comp (partial str path ":") name key) #(dissoc (val %) :elements)))
       (into (ordered-map))))

(defn flatten-profile
  [m prefix]
  (reduce-kv
   (fn [acc k v]
     (let [path (get-path prefix k)]
       (cond
         (= :extension k)          (merge acc (flatten-extension path v))
         (and
          (map? v)
          (contains? v :elements)) (merge (assoc acc path (dissoc v :elements))
                                          (flatten-profile (:elements v) path))
         :else                     (assoc acc path v))))
   (ordered-map) m))

(defn capitalize-first
  [s]
  (str (str/capitalize (subs s 0 1)) (subs s 1)))

(defn constants
  [element-id v]
  {(keyword (str "fixed" (capitalize-first (last (str/split element-id #"\."))))) v})

(def constraint-struct
  '(:requirements :severity :human :expression :xpath :source))

(defn fhirpath-rule
  [coll]
  {:constraint (mapv
                (fn [[k v]]
                  (reduce
                     (fn [acc k]
                       (into acc
                             (if (contains? v k)
                               {k (get v k)}
                               (cond
                                 (= k :severity) {k "error"}
                                 (= k :human) (if-let [str (get v :description)] {k str})))))
                     (ordered-map {:key (name k)}) constraint-struct))
                coll)})

(defn polymorphic-types
  [id path v]
  {:id (str id "[x]")
   :path (str path "[x]")
   :type (mapv
          (fn [type]
            {:code type})
          v)})

(defn mapping
  [maps]
  {:mapping (mapv (fn [[k v]]
                    (into (ordered-map {:identity (name k)}) v))
                  maps)})

(defn cardinality
  [k v]
  (condp = k
    :required (when v {:min 1})
    :disabled (when v {:max 0})
    :minItems {:min v}
    :maxItems {:max v}))

(defn add-defaults
  [map]
  (if (contains? map :mustSupport)
    map
    (into map {:mustSupport true})))

(defn refers
  ;;target url = https://healthsamurai.github.io/igpop/profiles/{resourceType}/basic.html
  [v]
  {:type
   (reduce (fn [outer-acc ordmap]
             (conj outer-acc
                   (reduce-kv (fn [acc key val]
                             (into acc (if (= key :resourceType)
                                         { :targetProfile [ (str "https://healthsamurai.github.io/igpop/profiles/" val "/basic.html") ] })))
                           (ordered-map {:code "Reference"}) ordmap)
                   )) [] v)})

(defn valueset
  [map]
  {:binding (reduce-kv (fn [acc k v]
                         (into acc
                               (cond
                                 (= k :id) {:valueSet (str "https://healthsamurai.github.io/igpop/valuesets/" v ".html")}
                                 (= k :description) {k v}
                                 (= k :strength) {k v})))
                       (ordered-map {:strength "extensible"}) map)})

(defn prop->sd [id path prop-k v]
  (cond
    (#{:disabled :required :minItems :maxItems} prop-k) (cardinality prop-k v)
    (#{:comment :definition :requirements :mustSupport} prop-k) {prop-k v}
    (= prop-k :constant) (constants id v)
    (= prop-k :constraints) (fhirpath-rule v)
    (= prop-k :union) (polymorphic-types id path v)
    (= prop-k :refers) (refers v)
    (= prop-k :valueset) (valueset v)
    (= prop-k :mappings) (mapping v)
    (= prop-k :description) {:short v}))

(defn element->sd [[el-key props]]
  (let [id (name el-key)
        colon-idx (or (str/index-of id ":") (count id))
        path (subs id 0 colon-idx)
        base (ordered-map {:id id :path path})]
    (->> props
         (map (juxt key val))
         (map (partial apply prop->sd id path))
         (reduce into base)
         add-defaults)))

(defn elements-to-sd
  [els]
  (map element->sd els))

(defn generate-differential
  [rt prn props]
  (let [flat-profile (flatten-profile (:elements props) (name rt))
        element (ordered-map {rt (dissoc props :elements)})
        elements (merge element flat-profile)]
    {:element (elements-to-sd elements)}))

(defn generate-snapshot
  [rt profile]
  (let [flat-profile (flatten-profile (:elements profile) (name rt))
        element (ordered-map {rt (dissoc profile :elements)})
        elements (merge element flat-profile)]
    {:element (elements-to-sd elements)}))

(defn generate-structure
  [{diffs :diff-profiles snapshot :snapshot :as ctx}]
  (let [m {:resourceType "Bundle"
           :id "resources"
           :meta {:lastUpdated (java.util.Date.)}
           :type "collection"}]
    (->> diffs
         (mapcat (fn [[rt prls]] (for [[prn props] prls] [rt prn props])))
         (map (fn [[rt prn props]]
                {:fullUrl  (str "baseUrl" "/" (name-that-profile rt prn))
                 :resource {:resourceType "StructureDefinition"
                            :id (name prn)
                            :description (:description props)
                            :type (name rt)
                            :snapshot (generate-snapshot rt (get-in snapshot [rt prn]))
                            :differential (generate-differential rt prn props)}}))
         (into [])
         (assoc m :entry))))


;; ----------------------------- PLAYGROUND ----------------------------

(set! *warn-on-reflection* true)

;; ---------------------------------------------------------------------

(defn capitalize
  "Uppercase first character of the string.
  Unlike `clojure.string/capitalize` - we don't lowercase rest of the string"
  [^String s]
  (str (.toUpperCase (subs s 0 1)) (subs s 1)))

(defn to-sd-path [parts]
  (str/join "." (map name parts)))

(defn poly-name-to-path-x
  "Convert path `parts` to path-x of polymorphic value
  Example: `[:A,:B,:C] -> A.B.C[x]`"
  [path] (str (to-sd-path path) "[x]"))

(defn poly-name-to-path
  "Convert path `parts` and `poly-value` to path of polymorphic value
  Example: `[:A,:B,:C] and value -> A.B.CValue`"
  [path poly-value]
  (str (to-sd-path path) (capitalize (name poly-value))))

;; ---------------------------------------------------------------------

(defn process-description
  "Rename :description key to :human"
  [item]
  (-> item
      (assoc :human (:description item))
      (dissoc :description)))

;; (process-description {:description "Hello"}) 


;; convert igpop constraints
;; ```
;; elements:
;;   name:
;;     constaints:
;;       us-core-8:
;;         expression: "family.exists() or given.exists()"
;;         description: "Patient.name.given or Patient.name.family or both SHALL be present"
;;         # severity: error (default)
;; ```
;;
;; into structure-definition constraints
;;
;; ```
;; constraints:
;; - severity: error
;;   key: us-core-8
;;   expression: "family.exists() or given.exists()"
;;   human: "Patient.name.given or Patient.name.family or both SHALL be present"
;; ```
(defn ig-constraint->sd-constraint
  [[item-name item]]
  (cond-> item
    :default                (assoc :key (name item-name))
    (:description item)     (process-description)
    (nil? (:severity item)) (assoc :severity "error")
    ;; TODO: which atrributes we need to process here?
    ;; TODO: make more generic  dispatch (instead of `cond->`)
    ))

(defn process-constraints
  "Process 'constraint' igpop logic on item"
  [item]
  (-> item
      (assoc :constraint (mapv ig-constraint->sd-constraint (:constraints item)))
      (dissoc :constraints)))

(defn cardinality-given?
  "Check are cardinality keys given"
  [item] (or (contains? item :disabled)
             (contains? item :required)
             (contains? item :minItem)
             (contains? item :maxItem)))

;; Cardinality-patterns should be checked earlier
;; we don't check XOR (exactly one) and try to find any of them: (and override values)
;;   - disabled true
;;   - required true
;;   - min and max
(defn process-cardinality
  "Process 'cardinality' igpop logic on item"
  [item]
  (cond-> item
    (:disabled item) (assoc :max 0)
    (:required item) (assoc :min 1)
    (:minItem  item) (assoc :min (:minItem item))
    (:maxItem  item) (assoc :max (:maxItem item))
    :default         (dissoc :disabled :required :minItem
                             :maxItem :collection)))

(defn process-constant
  "Process 'constant' igpop logic on item"
  [item]
  (let [key (keyword (str "fixed" (:type item)))]
    (-> item
        (assoc key (:constant item))
        (dissoc :constant))))

;; Convert igpop Polymorphic types
;; ```
;; elements:
;;   value:
;;     required: true
;;     union: [string CodeableConcept Quantity]
;;     string: {required: true}
;;     CodeableConcept:
;;       required: true
;;       valueset {id: 'vs'}
;; ```
;;
;; into several structure-definition polymorphic entries
;;
;; ```
;; - path: Observation.value[x]
;;   slicing:
;;     discriminator:
;;     - type: type
;;     path: "$this"
;;     ordered: false
;;     rules: closed
;;   type:
;;   - code: Quantity
;;   - code: string
;; - path: Observation.valueCodeableConcept
;;   binding: {valuset: 'http://....'}
;; ...
;; - path: Observation.valueString
;; ```

(defn process-valueset
  "Move valueset key-val to `:binding` attribute"
  [item]
  (-> item
      ;; what keys also need to put into :binding?
      (update :binding merge (select-keys item [:valueset]))
      (dissoc :valueset)))

;; (process-valueset {:valueset {:id "none"}})

;; can't find good name for this fn.
(defn poly-value-item [path [poly-name item]]
  (cond-> item
    :default         (assoc :path (poly-name-to-path path poly-name))
    (:valueset item) (process-valueset)))

(defn igpop-polymorphic->sd-polymorphic
  [{:keys [path]} item-name item]
  (let [union-types (:union item)
        union-defined (select-keys item (mapv keyword union-types))
        new-path (conj path item-name)]
    (vec (cons {:type (mapv (fn [t] {:code t}) union-types) ;; TODO: Move this map into something separate.
                :path (poly-name-to-path-x new-path)
                :slicing {:discriminator {:type "type"}
                          :path "$this"
                          :ordered false
                          :rules "closed"}}
               (mapv (partial poly-value-item new-path) union-defined)))))

;; TODO: make more generic  dispatch (instead of `cond->`)
(defn ig-item->sd-item
  "Convert `ig-pop-item` to `structure-definition-item`"
  [ctx item-name item]
  (cond-> item
    :default                   (assoc :path (to-sd-path (conj (:path ctx) item-name)))
    (nil? (:mustSupport item)) (assoc :mustSupport true)
    (cardinality-given? item)  (process-cardinality)
    (:constant item)           (process-constant)
    (:constraints item)        (process-constraints)))

;; ---------------------------------------------------------------------

;; TODO: make more generic  dispatch (instead of `cond->`)
;; We need to define some system of item characteristics
;; to distiguish them before processing  (which is `union` item / or `default`)

(defn to-sd-elements
  "Walk on elements converts items to structure definitions
   if there are some nested elements, go deeper (recursive function)

  returns context:
   {:path [path of current position]
    :result [converted elements]} "
  [ctx elements]
  (reduce-kv (fn [ctx item-name item]
            (cond-> ctx

              (:union item)
              (update :result into (igpop-polymorphic->sd-polymorphic ctx item-name item))

              ;; not a union and not top-level item
              (and (not (:union item)) (not= (:path ctx) []))
              (update :result conj (ig-item->sd-item ctx item-name (dissoc item :elements)))

              ;; nested elements
              (:elements item)
              (-> (update :path conj item-name)
                  (to-sd-elements (:elements item))
                  (assoc :path (:path ctx)))))  ;; reset path before next iteration
          ctx elements))

;; reset path to original version (:elements add parts to path)
;; ---------------------------------------------------------------------

(defn to-sd
  "Transform igpop nested structure to flat vector of
  structure-definitions and wraps them in `{:snapshot [definitions]}`"
  [igpop]
  {:snapshot (-> {:path [] :result []}
                 (to-sd-elements igpop)
                 :result)})

;; (to-sd-path [:Patient :name])

