(ns igpop.site.profiles
  (:require
   [igpop.site.views :as views]
   [clojure.string :as str]
   [garden.core :as gc]
   [clj-yaml.core]
   [igpop.site.utils :as u]))


(defn read-yaml [pth]
  (clj-yaml.core/parse-string
   (slurp pth)))

(def styles
  [:body
   [:.profile {:margin "0 20px"}]
   [:pre.example {:background-color "#f5f7f9"
                  :border "1px solid #f5f7f9"
                  :padding "15px"}]
   [:.vs {:font-size "12px"
          :text-decoration "underline"
          :color "#3b454e"}
    [:.fa {:font-size "9px"
           :opacity 0.7}]
    ]
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

   [:.activeS {:display "none"
               ;;:-webkit-transform "translateY(-100%)"
               ;;:transform "translateY(-100%)"
               ;;:transition "transform 3000ms linear"
               ;;:will-change "transform"
               }]

   [:.table {:display "table"
             :border-collapse "collapse"
             :margin-top "24px"
             :margin-bottom "32px"}]
   [:.row {:display "table-row"
           :border-bottom "1px solid #f1f1f1"}]
   [:.column {:display "table-cell"
              :padding "8px"}]
   [:.first-line {:border-bottom "2px solid #E6ECF1"
                  :color "#5b6975"}]

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
       [:&:hover {:background-color "#f5f7f9"}]]

      [:.el {:border-left link-border}
       [:&:last-of-type {:border-left-color "transparent"}
        [:.el-header  {:border-left-color "transparent"
                       :font-size "15px"
                       :line-height "30px"}]]]

      [:.down-link {:position "absolute"
                    :top "27px"
                    :bottom "0px"
                    :left "20px"
                    :width "0px"
                    :border-left link-border}]
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

(def type-symbols
  {"Reference" [:span.fa.fa-arrow-right]
   "date" [:span.fa.fa-calendar-day]
   "dateTime" [:span.fa.fa-clock]
   "Period" [:span.fa.fa-clock]
   "instant" [:span.fa.fa-clock]
   "Range" [:span.fa.fa-arrows-alt-h]
   "Distance" [:span.fa.fa-arrows-alt-h]
   "SampledData" [:span.fa.fa-wave-square]
   "time" [:span.fa.fa-clock]
   "Duration" [:span.fa.fa-clock]
   "Timing" [:span.fa.fa-calendar-check]
   "Address" [:span.fa.fa-home]
   "CodeableConcept" [:span.fa.fa-tags]
   "Coding" [:span.fa.fa-tag]
   "code" [:span.fa.fa-tag]
   "Identifier" [:span.fa.fa-fingerprint]
   "id" [:span.fa.fa-fingerprint]
   "uri" [:span.fa.fa-link]
   "canonical" [:span.fa.fa-link]
   "Signature" [:span.fa.fa-signature]
   "Attachment" [:span.fa.fa-file-download]
   "url" [:span.fa.fa-link]
   "Dosage" [:span.fa.fa-pills]
   "oid" [:span.fa.fa-fingerprint]
   "uuid" [:span.fa.fa-fingerprint]
   "Quantity" [:span.fa.fa-tachometer-alt]
   "Ratio" [:span.fa.fa-balance-scale]
   "HumanName" [:span.fa.fa-user]
   "Meta" [:span.fa.fa-info-circle]
   "boolean" [:span.fa.fa-toggle-on]
   "Money" [:span.fa.fa-dollar-sign]
   "base64Binary" [:span.fa.fa-file-archive]
   "integer" "Z" 
   "positiveInteger" "Z" 
   "Narrative" [:span.fa.fa-pen]
   "string" [:span.fa.fa-pen]
   "markdown" [:span.fa.fa-pen]
   "Annotation" [:span.fa.fa-pen]
   "ContactPoint" [:span.fa.fa-phone]})

(defn type-icon [nm el]
  (if-let [tp (:type el)]
    [:span.tp {:class (str tp (when (Character/isUpperCase (first tp)) " complex"))}
     (or (get type-symbols tp) (subs tp 0 1))]
    [:span.tp {:class "obj"} (cond
                               (= :extension nm) [:span.fa.fa-folder-plus]
                               (:union el) [:span.fa.fa-question-circle]
                               (:elements el) [:span.fa.fa-folder]
                               :else "?")]))
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

(defn has-children? [el]
  (or 
   (:elements el)
   (:union el)))

(defn get-children [nm el]
  (cond
    (:elements el) (:elements el)
    (and (= :extension nm)) el
    (:union el) (->> (:union el)
                     (reduce (fn [acc tp]
                               (assoc acc tp (merge (or (get el (keyword tp)) {})
                                                    {:type tp}))) {}))))

(defn element-row [ctx nm el]
  [:div.el-header
   [:span.link]
   (when (has-children? el)
     [:span.down-link])
   (type-icon nm el)
   [:div.el-line
    [:div.el-title nm (required-span el) " " (type-span el) (collection-span el)]
    [:div.desc
     (when-let [d (:description el)]
       [:span d " "])
     (when-let [vs (:valueset el)]
       [:a.vs {:href (u/href ctx "valuesets" (:id vs))}
        [:span.fa.fa-tag]
        " "
        (:id vs)]
       )
     ]]])

(defn new-elements [ctx elements]
  (->> elements
       (mapv (fn [[nm el]]
               [:div.el
                (element-row ctx nm el)
                (when-let [els (get-children nm el)]
                  (new-elements ctx els))]))
       (into [:div.el-cnt])))

(defn profiles-to-menu [{profiles :profiles :as ctx}]
  (->> profiles
       (mapv (fn [[rt nm]]
               {:display (name rt)
                :href (u/href ctx "profiles" (name rt) "basic" {:format "html"})
                :items (->> (keys nm)
                            (filter #(not (= :basic %)))
                            (map (fn [n]
                                   {:display (name n)
                                    :href (u/href ctx "profiles" (name rt)  (name n) {:format "html"})})))}))))

(defn profile [ctx {{rt :resource-type nm :profile} :route-params :as req}]
  (let [profile (get-in ctx [:profiles (keyword rt) (keyword nm)])]
    {:status 200
     :body (views/layout ctx
            style-tag
            (views/menu (profiles-to-menu ctx) req)
            [:div#content
             [:h1 rt " " [:span.sub (str/lower-case rt) "-" nm]]
             [:div.summary (:description profile)]
             [:hr]
             [:br]
             [:div.profile
              [:h5 [:div.tp.profile [:span.fa.fa-folder]] rt]
              (new-elements ctx (:elements profile))]

             [:br]
             [:br]
             [:h3 "Examples"]
             [:br]
             (for [[id example] (:examples profile)]
               [:div
                [:h5 id]
                [:pre.example [:code (clj-yaml.core/generate-string example)]]])]
            )}))

(defn profile-link [ctx rt nm pr]
  [:a.db-item {:href (u/href ctx "profiles" (name rt) (name nm) {:format "html"})}
   [:h5 (name rt) ":" (name nm)]
   [:div.desc (when (:description pr) (subs (:description pr) 0 (min (count (:description pr)) 55)))]])

(defn profiles-dashboard [ctx {{rt :resource-type nm :profile} :route-params :as req}]
  {:status 200
   :body (views/layout ctx
          style-tag
          (into [:div#db-content]
                (->> (:profiles ctx)
                     (sort-by first)
                     (mapcat
                      (fn [[rt profiles]]
                        (->> profiles
                             (mapv (fn [[nm pr]] (profile-link ctx rt nm pr)))))))))})







