(ns igpop.profile.profile
  (:require [flatland.ordered.map :refer :all]
            [clojure.pprint :as p]
            [clojure.string :as s]
            [igpop.profile.parse-rule :refer :all]))

(defn get-path
  [prefix key]
  (str prefix "." (name key)))

(defn flatten-profile
  [map prefix]
  (reduce
    (fn [acc [k v]]
      (if (map? v)
        (if (contains? v :elements)
          (merge (merge acc (ordered-map {(get-path prefix k) (dissoc v :elements)})) (flatten-profile (:elements v) (get-path prefix k)))
          (if (= :extension k)
            (reduce (fn [accum entry] (merge accum {(get-path (str prefix ".extension") (key entry)) (val entry)})) acc v)
            (merge acc (ordered-map {(get-path prefix k) v}))))
        (merge acc (ordered-map {(get-path prefix k) v}))))
    (ordered-map []) map))

(defn elements-to-sd
  [els]
  els
  (map (fn [[el-key props]]
         (reduce
           (fn [acc [rule-key rule-func]]
             (into acc
                   (if (contains? props rule-key)
                     (rule-func rule-key (get props rule-key))
                     (if (contains? default-agenda rule-key) (rule-func)))))
           (ordered-map {:id (name el-key) :path (name el-key)}) agenda))
       els))

(defn prepare-parse-metadata
  [profile-type
   profile-id
   diff-profile]
  (let [target (get-in diff-profile [profile-type profile-id])
        target-elements (:elements target)
        target-elements-parse-metadata [{:elements     target-elements
                                         :profile-type profile-type}]]
    (if-let [extension-elements (into (ordered-map []) (:extension target-elements))]
      (reduce (fn [acc extension]
                (conj acc {:elements     (conj (ordered-map []) extension)
                           :profile-type :Extension}))
              target-elements-parse-metadata extension-elements))))

(defn structure-definition
  [{differential-elements :elements profile-type :profile-type :as _}
   {resources :resources}]
  (let [basic-elements (get resources profile-type)
        snapshot-elements (merge basic-elements differential-elements)
        df (into (ordered-map []) (flatten-profile differential-elements (name profile-type)))]
    {:resourceType profile-type
     :id           "id"
     :snapshot     (elements-to-sd snapshot-elements)
     :differential (->
                     {}
                     (assoc :element (elements-to-sd df)))}))

(defn parsed-profile
  [profile-type
   profile-id
   {diffs :diff-profiles :as context}]
  (let [parse-metadata (prepare-parse-metadata profile-type profile-id diffs)]
    (map (fn [meta]
           (structure-definition meta context)) parse-metadata)
    ))