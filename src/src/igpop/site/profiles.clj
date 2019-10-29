(ns igpop.site.profiles
  (:require
   [igpop.site.views :as views]
   [clojure.string :as str]
   [clj-yaml.core]))

(defn enrich [base profile]
  (if-let [els (:elements profile)]
    (let [els' (reduce (fn [acc [k v]]
                         (if-let [base-element (get-in base [:elements k])]
                           (assoc acc k (enrich base-element v))
                           acc)) els els)]
      (assoc (merge (dissoc base :elements) profile) :elements els'))
    (merge (dissoc base :elements) profile)))

(defn current-page [uri res-url]
  (= uri res-url))

(defn menu [ctx {uri :uri}]
  [:div#main-menu
   (for [[rt profiles] (->> (:profiles ctx)
                            (sort-by first))]
     [:div
      (if (and (= 1 (count profiles))
               (= :basic (first (keys profiles))))
        (let [res-url (str "/profiles/" (name rt) "/basic")]
          [:a {:href res-url :class (when (current-page uri res-url) "active")} (name rt)])
        [:div
         [:a (name rt)]
         (into [:section]
               (for [[nm pr] profiles]
                 [:a {:href (str "/profiles/" (name rt) "/" (name nm))} (name nm)]))])])])

(def type-symbols
  {"Reference" [:span.fa.fa-arrow-right]
   "date" [:span.fa.fa-calendar-day]
   "dateTime" [:span.fa.fa-clock]
   "Period" [:span.fa.fa-clock]
   "instant" [:span.fa.fa-clock]
   "Address" [:span.fa.fa-home]
   "CodeableConcept" [:span.fa.fa-tags]
   "Coding" [:span.fa.fa-tag]
   "code" [:span.fa.fa-tag]
   "Identifier" [:span.fa.fa-fingerprint]
   "id" [:span.fa.fa-fingerprint]
   "HumanName" [:span.fa.fa-user]
   "string" [:span.fa.fa-pen]
   "Annotation" [:span.fa.fa-pen]
   "ContactPoint" [:span.fa.fa-phone]})

(defn type-icon [nm el]
  (if-let [tp (:type el)]
    [:span.tp {:class (str tp (when (Character/isUpperCase (first tp)) " complex"))}
     (or (get type-symbols tp) (subs tp 0 1))]
    [:span.tp {:class "obj"} (if (= :extension nm)
                               [:span.fa.fa-folder-plus]
                               (if (:elements el)
                                 [:span.fa.fa-folder]
                                 "?"))]))
(defn required-span [el]
  (when (or (:required el)
            (and (:minItems el)
                 (number? (:minItems el))
                 (> (:minItems el) 0)))
    [:span.required "*"]))

(defn type-span [el]
  (when-let [tp (:type el)]
    [:span.tp-link  tp]))

(defn collection-span [el]
  (when (:collection el)
    [:span.tp-link.coll (str "[" (or (:minItems el) 0) ".." (or (:maxItems el) "*") "]")]))

(defn element-row [nm el]
  [:div.el-header [:span.link]
   [:span.nm (type-icon nm el) nm (required-span el) " " (type-span el) (collection-span el)]
   [:div.desc (:description el)]])

(defn new-elements [elements]
  (->> elements
       (reduce (fn [acc [nm el]]
                 (conj acc
                       [:div.el
                        (element-row nm el)
                        (when-let [els (or (:elements el) (and (= :extension nm) el))]
                          (new-elements els))])
                 ) [:div.el-cnt])))


(defn profile [ctx {{rt :resource-type nm :profile} :route-params :as req}]
  (let [profile (get-in ctx [:profiles (keyword rt) (keyword nm)])
        base (read-yaml (str "../igpop-fhir-4.0.0/" rt ".yaml"))
        profile (enrich base profile)]
    {:status 200
     :body (views/layout
            (menu ctx req)
            [:div#content
             [:h1 rt " " [:span.sub (str/lower-case rt) "-" nm]]
             [:div.summary (:description profile)]
             [:hr]

             [:br]
             [:br]
             [:h5 [:div.tp.profile [:span.fa.fa-folder]] rt]
              (new-elements (:elements profile))

             [:br]
             [:h3 "Examples"]
             [:br]
             [:h3 "API"]
             ])}))

(defn profiles-dashboard [ctx {{rt :resource-type nm :profile} :route-params :as req}]
  {:status 200
   :body (views/layout
          (->> (:profiles ctx)
               (sort-by first)
               (reduce (fn [acc [rt profiles]]
                         (->> profiles
                              (reduce (fn [acc [nm pr]]
                                        (conj acc 
                                              [:a.db-item {:href (str "/profiles/" (name rt) "/" (name nm))}
                                               [:h5 (name rt) ":" (name nm)]
                                               [:div.desc (subs (:description pr) 0 (min (count (:description pr)) 55))]])

                                        ) acc))

                         ) [:div#db-content])))})







