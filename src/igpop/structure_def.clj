(ns igpop.structure-def
  (:require [flatland.ordered.map :refer :all]))

(defn name-that-profile [rt prn] (str (name rt) (when (not (= "basic" (name prn)))
                                                  (str "_" (name prn)))))

(defn generate-snapshot [] (-> {}
                               (assoc :element [(-> {})])))

(defn element-processing [acc el parent-name] (-> {}
                                                  (assoc :id (str parent-name "." (name el)))))

(defn get-path
  [prefix key v]
  (do
    (if (or (clojure.string/includes? prefix "DiagnosticReport") (clojure.string/includes? prefix "DiagnosticReport")) (do (clojure.pprint/pprint v) (println "/n")));;"prefix is    " prefix "  key is     " key ))
    (str prefix "." key)))

(defn flatten-profile
  [map prefix]
   ;;(dof
     ;;(println prefix)
     ;;(if (clojure.string/includes? prefix "Practitioner") (56))
     (reduce
      (fn [acc [k v]]
        (if (map? v)
          (if (contains? v :elements)
            (merge (merge acc (ordered-map {(get-path prefix (name k) acc) (dissoc v :elements)})) (flatten-profile (:elements v) (get-path prefix (name k) acc)) )
            (merge acc (ordered-map {(get-path prefix (name k) acc) v})))
          (merge acc (ordered-map {(get-path prefix (name k) acc) v}))))
        (ordered-map []) map));;)

(defn generate-differential [rt prn props] (-> {}
                                               (assoc :element [(into {} (flatten-profile (:elements props) (name rt)))])))

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
