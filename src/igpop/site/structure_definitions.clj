(ns igpop.site.structure-definitions
  (:require
   [igpop.site.views :as views]
   [clojure.string :as str]
   [garden.core :as gc]
   [clj-yaml.core]
   [cheshire.core :refer [generate-string]]
   [igpop.structure-definition :as sd]
   [igpop.site.utils :as u]))

(def styles
  [:body
   [:hr {:margin "25px 0 10px 0"}]
   [:.resource {:margin "0 20px"}]
   [:pre.example {:background-color "#f5f7f9"
                  :border "1px solid #f5f7f9"
                  :padding "15px"}]
   [:.table-link {:text-decoration "underline"
                  :color "#3b454e"}]
   [:.vs {:font-size "12px"
          :text-decoration "underline"
          :color "#3b454e"}
    [:.fa {:font-size "9px"
           :opacity 0.7}]]
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
    [:.fa {:padding-top "5px" :font-size "12px" :min-width "20px"}]
    [:&.complex :&.obj {:border-radius "3px" :min-width "20px"}]
    [:&.resource {:margin-left "-10px" :border-radius "3px"}]]

   ;;[:.el-cnt.activeS {:height "0px"}]

   [:.table {:display "table"
             :border-collapse "collapse"
             :margin-top "24px"
             :margin-bottom "32px"}]
   [:.row {:display "table-row"
           :border-bottom "1px solid #f1f1f1"}]
   [:.column {:display "table-cell"
              :font-size "15px"
              :padding "8px"}]
   [:.first-line {:border-bottom "2px solid #E6ECF1"
                  :color "#5b6975"}]

   (let [link-color "#b3bac0" ;;"#e6ecf0"
         link-border (str "1px solid " link-color)
         left-width 360]
     [:.el-cnt
      {;;:overflow-y "hidden"
       :color "rgb(33,37,41)"
       :font-weight "400"
       ;;:max-height "1400px"
       ;;:-webkit-transition "height 0.3s ease-in-out"
       ;;:-moz-transition "height 0.3s ease-in-out"
       ;;:-o-transition "height 0.3s ease-in-out"
       ;;:transition "height 0.3s ease-in-out"
       }

      [:.required {:color "red" :opacity 0.7 :margin "0 0.2em"}]
      [:.coll {:color "#888"}]
      [:.desc {:color "#5b6975" :font-size "14px" :line-height "23px"}]
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

      [:.el {:border-left link-border
             :overflow "visible"}
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
      [:.desc {:flex 1
               :min-width "370px"}]
      [:.el-cnt {:margin-left "20px"}
       [:.el-title {:width (str (- left-width 20) "px")}]
       [:.el-cnt
        [:.el-title {:width (str (- left-width 43) "px")}]
        [:.el-cnt
         [:.el-title {:width (str (- left-width 63) "px")}]
         [:.el-cnt
          [:.el-title {:width (str (- left-width 83) "px")}]
          [:.el-cnt
           [:.el-title {:width (str (- left-width 103) "px")}]]]]
        ]
       ]
      ])

    [:.navbar {:margin "0px"
               :display "flex"
               :padding "0px"
               :max-width "100%"
               :pointer-events "auto"
               :border-top-left-radius "3px"
               :border-top-right-radius "3px"}
     [:.navbutton {:color "rgb(157, 170, 182)"
                   :border-color "rgb(230, 236, 241) rgb(230, 236, 241) rgb(255, 255, 255) transparent"
                   :border-style "solid"
                   :border-width "1px"
                   :border-image "none 100% / 1 / 0 stretch"
                   :cursor "auto"
                   :margin "0px"
                   :flex "1 1 auto"
                   :outline "currentcolor none medium"
                   :padding "8px 0px"
                   :background-color "rgb(245, 247, 249)"
                   :transition "color 250ms ease-out 0s"}]
     [:.navbutton.tabActive {:color "rgb(36, 42, 49)"
                             :background-color "rgb(255, 255, 255)"}]
     [:.navbutton:hover {:color "rgb(36, 42, 49)"
                         :cursor "pointer"}]
     [:.navbutton:active {:color "red"
                          :transition "color 50ms ease-out 0s"}]
     [:.navbutton:first-child {:border-left-color "rgb(230, 236, 241)"
                               :border-top-left-radius "3px"}]
     [:.navbutton:last-child {:border-right-color "rgb(230, 236, 241)"
                              :border-top-right-radius "3px"}]
     [:.navtext {:padding "0 8px 0 8px"
                 :flex "1 1 16px"
                 :overflow "hidden"
                 :max-width "100%"
                 :text-overflow "ellipsis"
                 :line-height "1.5"
                 :font-weight "600"
                 :font-family "Content-font, Roboto, sans-serif"
                 :font-size "14px"}]]
   ])

(def style-tag [:style (gc/css styles)])

;; (def type-symbols
;;   {"Reference" [:span.fa.fa-arrow-right]
;;    "date" [:span.fa.fa-calendar-day]
;;    "dateTime" [:span.fa.fa-clock]
;;    "Period" [:span.fa.fa-clock]
;;    "instant" [:span.fa.fa-clock]
;;    "Range" [:span.fa.fa-arrows-alt-h]
;;    "Distance" [:span.fa.fa-arrows-alt-h]
;;    "SampledData" [:span.fa.fa-wave-square]
;;    "time" [:span.fa.fa-clock]
;;    "Duration" [:span.fa.fa-clock]
;;    "Timing" [:span.fa.fa-calendar-check]
;;    "Address" [:span.fa.fa-home]
;;    "CodeableConcept" [:span.fa.fa-tags]
;;    "Coding" [:span.fa.fa-tag]
;;    "code" [:span.fa.fa-tag]
;;    "Identifier" [:span.fa.fa-fingerprint]
;;    "id" [:span.fa.fa-fingerprint]
;;    "uri" [:span.fa.fa-link]
;;    "canonical" [:span.fa.fa-link]
;;    "Signature" [:span.fa.fa-signature]
;;    "Attachment" [:span.fa.fa-file-download]
;;    "url" [:span.fa.fa-link]
;;    "Dosage" [:span.fa.fa-pills]
;;    "oid" [:span.fa.fa-fingerprint]
;;    "uuid" [:span.fa.fa-fingerprint]
;;    "Quantity" [:span.fa.fa-tachometer-alt]
;;    "Ratio" [:span.fa.fa-balance-scale]
;;    "HumanName" [:span.fa.fa-user]
;;    "Meta" [:span.fa.fa-info-circle]
;;    "boolean" [:span.fa.fa-toggle-on]
;;    "Money" [:span.fa.fa-dollar-sign]
;;    "base64Binary" [:span.fa.fa-file-archive]
;;    "integer" "Z"
;;    "positiveInteger" "Z"
;;    "Narrative" [:span.fa.fa-pen]
;;    "string" [:span.fa.fa-pen]
;;    "markdown" [:span.fa.fa-pen]
;;    "Annotation" [:span.fa.fa-pen]
;;    "ContactPoint" [:span.fa.fa-phone]
;;    "Extension" [:span.fa.fa-align-left]
;;    "Complex" [:span.fa.fa-asterisk]})

;; (defn type-icon [nm el]
;;   (if-let [tp (:type el)]
;;     [:span.tp {:class
;;                (if (not-empty tp)
;;                  (str tp (when (Character/isUpperCase (first tp)) " complex"))
;;                  (str tp))}
;;      (or
;;       (get type-symbols tp)
;;       (when (str/includes? (str (:type el)) "Reference") [:span.fa.fa-arrow-right])
;;       (if (not-empty tp) (subs tp 0 1))
;;       "?")]
;;     [:span.tp {:class "obj"} (cond
;;                                (= :extension nm) [:span.fa.fa-folder-plus]
;;                                (= :Extension nm) [:span.fa.fa-align-left]
;;                                (:union el) [:span.fa.fa-question-circle]
;;                                (:slice el) [:span.fa.fa-layer-group]
;;                                (:elements el) [:span.fa.fa-folder]
;;                                :else "?")]))

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
  (or (:elements el)
      (:union el)
      (:slices el)))


;;(update (:slices el) :passport dissoc :constant)))
;; (update my-map :first-level dissoc :second-level)
(defn element-row [ctx nm el]
  [:div.el-header
   [:span.link]
   (when (or (has-children? el) (#{:Extension :extension} nm) (= :slices nm))
     [:span.down-link])
   ;; (type-icon nm el)
   [:div.el-line
    [:div.el-title nm (required-span el) " " (type-span el) (collection-span el)]
    [:div.desc
     [:div
      (when-let [d (:description el)]
        [:span d " "])
      (when-let [vs (:valueset el)]
        (let [href (or (:url vs)
                       (u/href ctx "valuesets" (str (:id vs) ".html")))]
          [:span
           [:a.vs {:href href}
            [:span.fa.fa-tag]
            " "
            (or (:url vs) (:id vs))]
           (if-let [s (:strength vs)]
             [:b {:style "font-size: 12px"} "&nbsp" s]
             [:b {:style "font-size: 12px"} "&nbspExtensible"])]))
      (when-let [url (:url el)]
        [:div [:b "URL:&nbsp"] [:a.vs {:href (u/to-local-href ctx url)} url] " "])
      (when-let [constant (:constant el)]
        [:div [:b "Constant:&nbsp"] constant " "])
      (when-let [match (:match el)]
        [:div [:b "Match:&nbsp"] match " "])
      (when-let [disabled (:disabled el)]
        [:div [:b "Disabled"] " "])]]]])

(defn new-elements [ctx elements]
  (->> elements
       (mapv (fn [[nm el]]
               [:div.el
                (element-row ctx nm el)
                #_(when-let [els (get-children nm el)]
                  (new-elements ctx els))]))
       (into [:div.el-cnt])))

(defn structure-definitions-to-menu [{resources :structure-definitions :as ctx}]
  (->> resources
       keys
       (map (fn [id]
              {:display id
               :href (u/href ctx "StructureDefinition" id {:format "html"})}))))

(defn- capitalize
  "Uppercase first character of the string.
  Unlike `clojure.string/capitalize` - we don't lowercase rest of the string"
  [^String s]
  (str (.toUpperCase (subs s 0 1)) (subs s 1)))

(defn render-table [ctx resource]
  (conj [:div.table
         [:div.row.th.first-line
          [:div.column
           #_[:div.th "Property"]]
          [:div.column
           #_[:div.th "Value"]]]]
        (->> resource
             (remove (comp #{:differential :snapshot :resourceType
                          :id :derivation :kind :experimental :context} key))
             (map (fn [[k v]]
                    [:div.row
                     [:div.column (capitalize (name k))]
                     [:div.column (cond
                                    (= k :url)
                                    [:span [:a.table-link {:href (u/to-local-href ctx (str v ".html"))} v]]
                                    (and (string? v) (str/starts-with? v "http"))
                                    [:span [:a.table-link {:href v} v]]

                                    :else (str v))]])))))

(defn extension? [resource]
  (-> resource :type (= "Extension")))

(defn structure-definition [ctx {{sd-id :sd-id} :route-params :as req}]
  (let [resource (get-in ctx [:structure-definitions sd-id])]
    {:status 200
     :body (views/layout
            ctx
            style-tag
            (views/menu (structure-definitions-to-menu ctx) req)
            [:div#content
             [:h2 "StructureDefinition "][:h1  sd-id]
             [:div.summary (:description resource)]
             [:hr]
             [:div.navbar
              [:button#info-tab.navbutton.tabActive {:onClick "openTab('info')"}
               [:div.navtext "Info "]]
              [:button#json-tab.navbutton {:onClick "openTab('json')"}
               [:div.navtext "JSON"]]]
             [:div#info.treecontainer
              [:br]
              [:h4 (str "Resource " (if (extension? resource)
                                      "Extension"
                                      "Profile") " Info")]
              [:div (str "The official URL for this "
                         (if (extension? resource) "extension" "profile") " is: ")
               [:a.table-link {:href (u/to-local-href ctx (str (:url resource) ".html"))} (:url resource)]  ]
              ;; [:br]
              (when-not (extension? resource)
                (let [derived (:baseDefinition resource)]
                  [:div #_[:br]
                   [:div "This structure is derived from "
                    [:a.table-link {:href (u/to-local-href ctx derived)} (last (str/split derived #"/"))]]]))
              ;; [:br]
              [:div.resource
               (render-table ctx resource)
               [:br]
               (when-let [extensions false] ;; TODO Add gathering extensions urls
                 [:div [:br]
                  [:div "Extensions"
                   [:ul (for [ex extensions]
                          [:li [:a.vs ex]])]]])
               #_[:h5 [:div.tp.profile [:span.fa.fa-folder]] sd-id]]]
             [:div#json.treecontainer {:style "display: none;"}
              [:br]
              [:h4 "JSON Representation "]
              [:br]
              [:div
               [:a {:href (u/href ctx "StructureDefinition" sd-id {:format "json"}) :no-download "true"}
                [:span.table-link  "Raw Json " [:span.fa.fa-eye]]]
               [:span " | "]
               [:a {:href (u/href ctx "StructureDefinition" sd-id {:format "json"}) :download (str sd-id ".json")}
                [:span.table-link  "Download " [:span.fa.fa-download]]]]
              [:br]
              [:div.profile
               [:pre.example (generate-string resource {:pretty true})]]]])}))


(defn structure-definition-link [ctx sd-id resource]
  [:a.db-item {:href (u/href ctx "StructureDefinition" sd-id {:format "html"})}
   [:h5 sd-id]
   [:div.desc
    (when (:description resource)
      (subs (:description resource) 0 (min (count (:description resource)) 55)))]])

(defn structure-definitions-dashboard [ctx {{sd-id :sd-id} :route-params :as req}]
  {:status 200
   :body (views/layout ctx
          style-tag
          (into [:div#db-content]
                (->> (:structure-definitions ctx)
                     (filter #(-> % val :resourceType (= "StructureDefinition")))
                     (sort-by first)
                     (map (fn [[id resource]] (structure-definition-link ctx id resource))))))})

