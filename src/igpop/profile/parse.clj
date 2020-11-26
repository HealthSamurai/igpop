(ns igpop.profile.parse
  (:require
    [flatland.ordered.map :refer :all]
    [clojure.string :as s]))

(defn ->intermediate-representation
  [map prefix]
  (reduce
    (fn [acc [k v]]
      (if (map? v)
        (if (contains? v :elements)
          (merge
            (merge acc (ordered-map {(conj prefix k) (dissoc v :elements)}))
            (->intermediate-representation (:elements v) (conj prefix k)))
          (merge acc (ordered-map {(conj prefix k) v})))
        (merge acc (ordered-map {(conj prefix k) v}))))
    (ordered-map []) map))

(defn ->extension-slice
  [element-name {el-type     :type
                 description :description
                 :as         element-data}]
  (let [base-slice (keyword (name element-name))
        url-slice (keyword (str (name element-name) ".url"))
        value-slice (keyword (str (name element-name) ".value" (s/capitalize el-type)))]
    (ordered-map {base-slice  {:sliceName  element-name
                               :definition description
                               :type       "Extension"}
                  url-slice   {:minItems 1,
                               :maxItems "1",
                               :type     "uri",
                               :fixedUri element-name}
                  value-slice {:minItems 1,
                               :maxItems "1"
                               :type     el-type}})))

(defn extension-defaults
  [element-name {el-type     :type
                 description :description
                 :as         element-data}]
  (ordered-map {nil        {:definition description}
                :url       {:minItems 1,
                            :maxItems "1",
                            :type     "uri",
                            :fixedUri element-name}
                "value[x]" {:minItems 0,
                            :maxItems "0"}}))


(defn- ->domain-resource-extension
  [element]
  (let [elem-key (key element)]
    (if (= :extension elem-key)
      (reduce (fn [acc el]
                (conj acc {(->> (key el)
                                (str "extension")
                                keyword) {:sliceName           (name (key el))
                                          :extension-reference (name (key el))}}))
              {} (val element))
      element)))

(defn denormalize-document
  [profile-type
   profile-id
   diff-profile]
  (let [target (get-in diff-profile [profile-type profile-id])
        target-elements (:elements target)

        denormalized-target-elements (reduce (fn [acc element]
                                               (merge acc (->domain-resource-extension element)))
                                             {} target-elements)
        target-elements-parse-metadata [{:elements     denormalized-target-elements
                                         :profile-type profile-type
                                         :type         :DomainResource
                                         :id           (name profile-id)}]]
    (if-let [extension-elements (into (ordered-map []) (:extension target-elements))]
      (reduce (fn [acc extension]
                (let [id (key extension)
                      elements (:elements (val extension))
                      extension-slice (reduce (fn [acc element]
                                                (merge acc (->extension-slice (key element) (val element))))
                                              {} elements)
                      default (reduce (fn [acc element]
                                        (merge acc (extension-defaults (key element) element)))
                                      {} elements)]
                  (conj acc {:elements     (conj (ordered-map []) extension-slice default)
                             :profile-type :Extension
                             :type         :Extension
                             :id           (name id)})))
              target-elements-parse-metadata extension-elements))))