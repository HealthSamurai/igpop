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
  (let [flatten-extension (fn [accum entry]
                            (let [path (get-path (str prefix ".extension") (key entry))]
                              (merge accum {path
                                            {:extension {:name  (name (key entry))
                                                         :id    path
                                                         :entry entry}}})))]
    (reduce
      (fn [acc [k v]]
        (if (map? v)
          (if (contains? v :elements)
            (merge
              (merge acc (ordered-map {(get-path prefix k) (dissoc v :elements)}))
              (flatten-profile (:elements v) (get-path prefix k)))
            (if (= :extension k)
              (reduce flatten-extension acc v)
              (merge acc (ordered-map {(get-path prefix k) v}))))
          (merge acc (ordered-map {(get-path prefix k) v}))))
      (ordered-map []) map)))

(defn elements-to-sd
  [id el-type els]
  els
  (let [elements (reduce
                   (fn [acc [el-key props]]
                     (conj acc (reduce
                                 (fn [acc [rule-key rule-func]]
                                   (into acc
                                         (if (contains? props rule-key)
                                           (rule-func rule-key (get props rule-key)))))
                                 {:id (name key) :path (name key)} domain-resource-agenda))) [] els)]
    elements))


(defn prepare-parse-metadata
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
          (fn[meta context] (meta :type)))

(defmethod structure-definition :DomainResource
  [{differential-elements :elements profile-type :profile-type type :type}
   {resources :resources}]
  (let [basic-elements (get resources profile-type)
        snapshot-elements (merge basic-elements differential-elements)
        differential (into (ordered-map []) (flatten-profile differential-elements (name profile-type)))
        snapshot (into (ordered-map []) (flatten-profile snapshot-elements (name profile-type)))]
    {:resourceType profile-type
     :id           "id"
     :snapshot     (-> {}
                       (assoc :element (elements-to-sd "id" type snapshot)))
     :differential (-> {}
                       (assoc :element (elements-to-sd "id" type differential)))
     }))

(defmethod structure-definition :Extension
  [{differential-elements :elements profile-type :profile-type type :type}
   {resources :resources}]
  (let [basic-elements (get resources profile-type)
        snapshot-elements (merge basic-elements differential-elements)
        differential (into (ordered-map []) (flatten-profile differential-elements (name profile-type)))
        snapshot (into (ordered-map []) (flatten-profile snapshot-elements (name profile-type)))]
    {:resourceType profile-type
     :id           "id"
     :snapshot     (-> {}
                       (assoc :element (elements-to-sd "id" type snapshot)))
     :differential (-> {}
                       (assoc :element (elements-to-sd "id" type differential)))
     }))

(defn parsed-profile
  [profile-type
   profile-id
   {diffs :diff-profiles :as context}]
  (let [parse-metadata (prepare-parse-metadata profile-type profile-id diffs)]
    (map (fn [meta]
           (structure-definition meta context)) parse-metadata)
    (structure-definition (first parse-metadata) context)
    ))