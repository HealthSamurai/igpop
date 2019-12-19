(ns igpop.structure-def
  (:require [flatland.ordered.map :refer :all]))

(defn name-that-profile [rt prn] (str (name rt) (when (not (= "basic" (name prn)))
                                                  (str "_" (name prn)))))

(defn generate-snapshot [] (-> {}
                               (assoc :element [(-> {})])))

(defn element-processing [acc el parent-name] (-> {}
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

(defn cardinality [k v] (cond
                             (= k :required) (if (= true v) {:min 1})
                             (= k :disabled) (if (= true v) {:max 0})
                             (= k :minItems) {:min v}
                             (= k :maxItems) {:max v}))

(def agenda {:required cardinality
             :disabled cardinality
             :minItems cardinality
             :maxItems cardinality})

(defn elements-to-sd
  [els]
  (map (fn [[el-key props]]
         (reduce
          (fn [acc [rule-key rule-func]]
            (into acc (if (contains? props rule-key) (rule-func rule-key (get props rule-key)))))
          (ordered-map {:id (name el-key) :path (name el-key)}) agenda))
       els))

(defn generate-differential [rt prn props] (-> {}
                                               (assoc :element (elements-to-sd (into (ordered-map []) (flatten-profile (:elements props) (name rt)))))))

(defn generate-structure [{diffs :diff-profiles profiles :profiles :as ctx}]
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

(defn to-sd [igpop]
  {:snapshot {:element [{:path "Patient.name"}]}})
