(ns igpop.profile.translate-profile
  (:require [flatland.ordered.map :refer :all]
            [clojure.pprint :as p]
            [clojure.string :as s]
            [igpop.profile.to-json-rule :as json-rule]
            [igpop.profile.parse :as parse]))


(defn translate
  [translation-rules profile-id els]
  els
  (map (fn [[el-key props]]
         (reduce
           (fn [acc [rule-key rule-func]]
             (into acc
                   (if (contains? props rule-key)
                     (rule-func profile-id rule-key (get props rule-key)))))
           (ordered-map {:id   (json-rule/->str-path el-key)
                         :path (json-rule/->str-path el-key)})
           translation-rules))
       els))


(defn json-structure-def
  [profile-id
   {differential-elements :elements
    profile-type          :profile-type
    type                  :type}
   {resources :resources}]
  (let [basic-elements (get resources profile-type)
        snapshot-elements (merge basic-elements differential-elements)
        differential (into (ordered-map []) (parse/->intermediate-representation differential-elements [profile-type]))
        snapshot (into (ordered-map []) (parse/->intermediate-representation snapshot-elements [profile-type]))]
    {:resourceType profile-type
     :id           (name profile-id)
     :snapshot     (-> {}
                       (assoc :element (translate json-rule/agenda profile-id snapshot)))
     :differential (-> {}
                       (assoc :element (translate json-rule/agenda profile-id differential)))}))

(defn ->json
  [profile-type
   profile-id
   {diffs :diff-profiles :as context}]
  (let [denormalized (parse/denormalize-document profile-type profile-id diffs)]
    (mapv (fn [meta]
            (json-structure-def profile-id meta context))
          denormalized)))