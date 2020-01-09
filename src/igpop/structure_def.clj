(ns igpop.structure-def
  (:require [flatland.ordered.map :refer :all]
            [clojure.string :as str]))

(defn name-that-profile
  [rt prn]
  (str (name rt) (when (not (= "basic" (name prn)))
                   (str "_" (name prn)))))

(defn generate-snapshot
  []
  (-> {}
      (assoc :element [(-> {})])))

(defn element-processing
  [acc el parent-name]
  (-> {}
      (assoc :id (str parent-name "." (name el)))))

(defn get-path
  [prefix key]
  (str prefix "." (name key)))

(defn flatten-profile
  [map prefix]
  (reduce
   (fn [acc [k v]]
     (if (map? v)
       (if (contains? v :elements)
         (merge (merge acc (ordered-map {(get-path prefix k) (dissoc v :elements)})) (flatten-profile (:elements v) (get-path prefix k)) )
         (merge acc (ordered-map {(get-path prefix k) v})))
       (merge acc (ordered-map {(get-path prefix k) v}))))
   (ordered-map []) map))

(defn capitalize-first
  [s]
  (str (clojure.string/capitalize (subs s 0 1)) (subs s 1)))

(defn constants
  [k v]
  {(keyword (str "fixed" (capitalize-first (last (clojure.string/split (name k) #"\."))))) v})

(def constraint-struct
  '(:requirements :severity :human :expression :xpath :source))

(defn fhirpath-rule
  [coll]
  {:constraint (mapv
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

(defn cardinality
  [k v]
  (cond
    (= k :required) (if (= true v) {:min 1})
    (= k :disabled) (if (= true v) {:max 0})
    (= k :minItems) {:min v}
    (= k :maxItems) {:max v}))

(defn add-defaults
  [map]
  (if (not (contains? map :mustSupport))
    (into map (mustSupport))))

(defn refers
  ;;target url = https://healthsamurai.github.io/igpop/profiles/{resourceType}/basic.html
  [k v]
  {:type
   (reduce (fn [outer-acc ordmap]
             (conj outer-acc
                   (reduce (fn [acc [key val]]
                             (into acc (if (= key :resourceType)
                                         { :targetProfile [ (str "https://healthsamurai.github.io/igpop/profiles/" val "/basic.html") ] })))
                           (ordered-map {:code "Reference"}) ordmap)
                   )) [] v)})

(defn elements-to-sd
  [els]
  (map (fn [[el-key props]]
         (add-defaults
          (reduce
           (fn [acc [prop-k v]]
             (into acc
                   (cond
                     (= prop-k (or :required :disabled :minItems :maxItems)) (cardinality prop-k v)
                     (= prop-k :constant) (constants el-key v)
                     (= prop-k :constraints) (fhirpath-rule v)
                     (= prop-k :mustSupport) (mustSupport v)
                     (= prop-k :refers) (refers prop-k v))))
           (ordered-map {:id (name el-key) :path (name el-key)}) props)))
       els))

(defn generate-differential
  [rt prn props]
  (-> {}
      (assoc :element (elements-to-sd (into (ordered-map []) (flatten-profile (:elements props) (name rt)))))))

(defn generate-structure
  [{diffs :diff-profiles profiles :profiles :as ctx}]
  (let [m {:resourceType "Bundle"
           :id "resources"
           :meta {:lastUpdated (java.util.Date.)}
           :type "collection"}]
    (assoc m :entry
           (into [] (apply concat (for [[rt prls] diffs]
                                    (for [[prn props] prls]
                                      (-> {}
                                          (assoc :fullUrl (str "baseUrl" "/" (name-that-profile rt prn)))
                                          (assoc :resource (-> {}
                                                               (assoc :resourceType "StructureDefinition")
                                                               (assoc :id (name prn))
                                                               (assoc :description (:description props))
                                                               (assoc :type (name rt))
                                                               (assoc :snapshot (generate-snapshot))
                                                               (assoc :differential (generate-differential rt prn props))))))))))))


;; ----------------------------- PLAYGROUND ----------------------------

;; TODO: move this to `ns` macro
(require '[clojure.string :as s])

(set! *warn-on-reflection* true)

;; ---------------------------------------------------------------------

(defn capitalize
  "Uppercase first character of the string.
  Unlike `clojure.string/capitalize` - we don't lowercase rest of the string"
  [^String s]
  (str (.toUpperCase (subs s 0 1)) (subs s 1)))

(defn to-sd-path [parts]
  (s/join "." (map name parts)))

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

(defn process-description [item]
  "Rename :description key to :human"
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

(defn process-constraints [item]
  "Process 'constraint' igpop logic on item"
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
(defn process-cardinality [item]
  "Process 'cardinality' igpop logic on item"
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

(defn process-valueset [item]
  "Move valueset key-val to `:binding` attribute"
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
(defn ig-item->sd-item [ctx item-name item]
  "Convert `ig-pop-item` to `structure-definition-item`"
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
  (reduce (fn [ctx [item-name item]]
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

