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


(defn structure-definitions-to-menu [ctx]
  (->> ctx
       :generated
       :structure-definitions
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
          #_[:div.column [:div.th "Property"]]
          #_[:div.column [:div.th "Value"]]]]
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


(defn extract-extension-urls [resource]
  (when (sd/sd-profile? resource)
    (->> resource
         :differential
         :element
         (filter (fn [el] (and (= 1 (count (:type el)))
                              (= "Extension" (get-in el [:type 0 :code])))))
         (map (fn [el] (get-in el [:type 0 :profile 0]))))))

(defn structure-definition [ctx {{sd-id :sd-id} :route-params :as req}]
  (let [resource (get-in ctx [:generated :structure-definitions sd-id])]
    {:status 200
     :body (views/layout
            ctx
            style-tag
            (views/menu (structure-definitions-to-menu ctx) req)
            [:div#content
             [:h2 "StructureDefinition "]
             [:h1 sd-id]
             [:div.summary (:description resource)]
             [:hr]
             [:div.navbar
              [:button#info-tab.navbutton.tabActive {:onClick "openTab('info')"}
               [:div.navtext "Info "]]
              [:button#json-tab.navbutton {:onClick "openTab('json')"}
               [:div.navtext "JSON"]]]
             [:div#info.treecontainer
              [:br]
              [:h4 (format "Resource %s Info" (if (sd/sd-extension? resource)
                                                "Extension"
                                                "Profile"))]
              [:div (format "The official URL for this %s is: " (if (sd/sd-extension? resource)
                                                                  "extension"
                                                                  "profile"))
               [:a.table-link {:href (u/to-local-href ctx (str (:url resource) ".html"))} (:url resource)]  ]
              (when-not (sd/sd-extension? resource)
                (let [derived (:baseDefinition resource)]
                  [:div #_[:br]
                   [:div "This structure is derived from "
                    [:a.table-link {:href (u/to-local-href ctx derived)} (last (str/split derived #"/"))]]]))
              ;; [:br]
              [:div.resource
               (render-table ctx resource)
               (when-let [extensions-urls (extract-extension-urls resource)]
                 [:div #_ [:br]
                  [:div [:h4 "Extensions"]
                   [:ul (for [url extensions-urls]
                          [:li [:a.table-link  {:href (u/to-local-href ctx (str url ".html"))} url]])]]])
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
                (->> ctx
                     :generated
                     :structure-definitions
                     (sort-by first)
                     (map (fn [[id resource]] (structure-definition-link ctx id resource))))))})

