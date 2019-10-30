(ns igpop.site.profiles
  (:require
   [igpop.site.views :as views]
   [clojure.string :as str]
   [garden.core :as gc]
   [clj-yaml.core]))


(defn read-yaml [pth]
  (clj-yaml.core/parse-string
   (slurp pth)))

(def styles
  [:body
   [:.profile {:margin "0 20px"}]
   [:.tp {:position "relative"
          :margin-top "5px"
          :z-index 10
          :display "inline-block"
          :opacity 0.6
          :font-size "10px"
          :margin-right "4px"
          :background-color "white"
          :box-shadow "0px 0px 2px black"
          :color "#5b6975"
          :font-weight "800"
          :vertical-align "middle"
          :line-height "20px"
          :text-align "center"
          :width "20px" :height "20px"
          :border-radius "20px"}
    [:.fa {:padding-top "5px" :font-size "12px"}]
    [:&.complex :&.obj {:border-radius "3px"}]
    [:&.profile {:margin-left "-10px" :border-radius "3px"}]]
   (let [link-color "#b3bac0" ;;"#e6ecf0"
         link-border (str "1px solid " link-color)
         left-width 360]
     [:.el-cnt
      {:color "rgb(33,37,41)"
       :font-weight "400"}

      [:.required {:color "red" :opacity 0.7 :margin "0 0.2em"}]
      [:.coll {:color "#888"}]
      [:.desc {:color "#5b6975" :font-size "14px"}]
      [:.tp-link {:font-size "13px" :color "#909aa2"}]
      [:.el-header {:padding-left "10px"
                    :position "relative"
                    :display "flex"
                    :flex-direction "row"
                    :justify-content "flex-start"
                    :line-height "30px"
                    :border-left link-border
                    :margin-left "-1px"}
       
       [:&:last-of-type {:border-left-color "transparent"}]]
      [:.el-line {:display "flex"
                  :flex-direction "row"
                  :padding-left "0.6em"
                  :flex 1
                  :justify-content "flex-start"
                  :border-bottom "1px solid #f1f1f1"}
       [:&:hover {:background-color "#f5f7f9"}]
       ]

      [:.el {:border-left link-border}
       [:&:last-of-type {:border-left-color "transparent"}
        [:.el-header  {:border-left-color "transparent"
                       :font-size "15px"
                       :line-height "30px"}]]]
      
      [:.down-link {
                    :position "absolute"
                    :top "27px"
                    :bottom "0px"
                    :left "20px"
                    :width "0px"
                    :border-left link-border
                    }]
      [:.link
       {:width "10px"
        :height "17px"
        :position "absolute"
        :top "0px" 
        :left "-1px" 
        :border-bottom link-border 
        :border-left link-border}]
      [:.el-title
       {:width (str left-width "px")
        :color "rgb(59, 69, 78)"
        :font-size "15px"}]
      [:.desc {:flex 1}]
      [:.el-cnt {:margin-left "20px"}
       [:.el-title {:width (str (- left-width 20) "px")}]
       [:.el-cnt
        [:.el-title {:width (str (- left-width 40) "px")}]]]])])

(def style-tag [:style (gc/css styles)])

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
   "Narrative" [:span.fa.fa-pen]
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
    [:span.tp-link.coll (str " [" (or (:minItems el) 0) ".." (or (:maxItems el) "*") "]")]))

(defn element-row [nm el]
  [:div.el-header
   [:span.link]
   (when (:elements el)
     [:span.down-link])
   (type-icon nm el)
   [:div.el-line
    [:div.el-title nm (required-span el) " " (type-span el) (collection-span el)]
    [:div.desc (:description el)]]])

(defn new-elements [elements]
  (->> elements
       (mapv (fn [[nm el]]
               [:div.el
                (element-row nm el)
                (when-let [els (or (:elements el) (and (= :extension nm) el))]
                  (new-elements els))]))
       (into [:div.el-cnt])))


(defn profile [ctx {{rt :resource-type nm :profile} :route-params :as req}]
  (let [profile (get-in ctx [:profiles (keyword rt) (keyword nm)])
        base (read-yaml (str "../igpop-fhir-4.0.0/" rt ".yaml"))
        profile (enrich base profile)]
    {:status 200
     :body (views/layout
            style-tag
            (menu ctx req)
            [:div#content
             [:h1 rt " " [:span.sub (str/lower-case rt) "-" nm]]
             [:div.summary (:description profile)]
             [:hr]
             [:br]
             [:div.profile
              [:h5 [:div.tp.profile [:span.fa.fa-folder]] rt]
              (new-elements (:elements profile))]

             [:br]
             [:h3 "Examples"]
             [:br]
             [:h3 "API"]
             ])}))

(defn profile-link [rt nm pr]
  [:a.db-item {:href (str "/profiles/" (name rt) "/" (name nm))}
   [:h5 (name rt) ":" (name nm)]
   [:div.desc (subs (:description pr) 0 (min (count (:description pr)) 55))]])

(defn profiles-dashboard [ctx {{rt :resource-type nm :profile} :route-params :as req}]
  {:status 200
   :body (views/layout
          style-tag
          (into [:div#db-content]
                (->> (:profiles ctx)
                     (sort-by first)
                     (mapcat
                      (fn [[rt profiles]]
                        (->> profiles
                             (mapv (fn [[nm pr]] (profile-link rt nm pr)))))))))})







