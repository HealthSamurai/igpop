(ns igpop.profile.profile
  (:require [flatland.ordered.map :refer :all]
            [clojure.pprint :as p]
            [clojure.string :as s]
            [igpop.profile.parse-rule :refer :all]))

(defn flatten-profile
  [map prefix]
  (let [flatten-extension (fn [accum entry]
                            (let [path (conj prefix :extension (keyword (key entry)))]
                              (merge accum (ordered-map {path
                                                         {:extension {:name  (name (key entry))
                                                                      :id    path
                                                                      :entry (val entry)}}}))))]
    (reduce
      (fn [acc [k v]]
        (if (map? v)
          (if (contains? v :elements)
            (merge
              (merge acc (ordered-map {(conj prefix k) (dissoc v :elements)}))
              (flatten-profile (:elements v) (conj prefix k)))
            (if (= :extension k)
              (reduce flatten-extension acc v)
              (merge acc (ordered-map {(conj prefix k) v}))))
          (merge acc (ordered-map {(conj prefix k) v}))))
      (ordered-map []) map)))


(defn domain-resource->structure-definition
  [els]
  els
  (map (fn [[el-key props]]
         (reduce
           (fn [acc [rule-key rule-func]]
             (into acc
                   (if (contains? props rule-key)
                     (rule-func rule-key (get props rule-key))
                     (if (contains? default-agenda rule-key) (rule-func)))))
           (ordered-map {:id (->str-path el-key) :path (->str-path el-key)})
           domain-resource-agenda))
       els))

(defn extension-diff->structure-definition
  [els]
  (into [] (reduce (fn [acc [el-key props]]
                     (concat acc (parse-extension-diff el-key props)))
                   [] els)))

(defn extension-snapshot->structure-definition
  [els]
  (into [] (reduce (fn [acc [el-key props]]
                     (concat acc (parse-extension-snapshot el-key props)))
                   [] els)))

(defn extract-resources-to-parse
  [profile-type
   profile-id
   diff-profile]
  (let [target (get-in diff-profile [profile-type profile-id])
        target-elements (:elements target)
        target-elements-parse-metadata [{:elements     target-elements
                                         :profile-type profile-type
                                         :type         (general-type profile-type)}]]
    (if-let [extension-elements (into (ordered-map []) (:extension target-elements))]
      (reduce (fn [acc extension]
                (conj acc {:elements     (conj (ordered-map []) extension)
                           :profile-type :Extension
                           :type         (general-type :Extension)}))
              target-elements-parse-metadata extension-elements))))

(defmulti structure-definition
          (fn [meta profile-id context] (meta :type)))

(defmethod structure-definition :DomainResource
  [{differential-elements :elements profile-type :profile-type type :type}
   profile-id
   {resources :resources}]
  (let [basic-elements (get resources profile-type)
        snapshot-elements (merge basic-elements differential-elements)
        differential (into (ordered-map []) (flatten-profile differential-elements [profile-type]))
        snapshot (into (ordered-map []) (flatten-profile snapshot-elements [profile-type]))]
    {:resourceType profile-type
     :id           (name profile-id)
     :snapshot     (-> {}
                       (assoc :element (domain-resource->structure-definition snapshot)))
     :differential (-> {}
                       (assoc :element (domain-resource->structure-definition differential)))}))

(defmethod structure-definition :Extension
  [{differential-elements :elements profile-type :profile-type type :type}
   profile-id
   {resources :resources}]
  (let [basic-elements (get resources profile-type)
        snapshot-elements (merge basic-elements differential-elements)
        differential (into (ordered-map []) (flatten-profile differential-elements [profile-type]))
        snapshot (into (ordered-map []) (flatten-profile snapshot-elements [profile-type]))]
    {:resourceType profile-type
     :id           (name profile-id)
     :snapshot     (-> {}
                       (assoc :element (extension-snapshot->structure-definition snapshot)))
     :differential (-> {}
                       (assoc :element (extension-diff->structure-definition differential)))}))

(defn parsed-profile
  [profile-type
   profile-id
   {diffs :diff-profiles :as context}]
  (let [parse-metadata (extract-resources-to-parse profile-type profile-id diffs)]
    (mapv (fn [meta]
           (structure-definition meta profile-id context))
         parse-metadata)))