(comment (ns profile-site.stuff
           (:require [hiccup.core :as hc]
                     [profile-site.style :as pss]
                     [profile-site.data :as psd]
                     [profile-site.utils :refer :all]
                     [profile-site.views :refer :all]))

         (defn attrs->hc [profile]
           (vec (map (fn [itm]
                       (vec [:div.row
                             [:div.col
                              [:div (assoc {:class "line-item"} :style (when (> (count (*get itm 1)) 0)
                                                                         "background-image: url(/assets/tbl_bck11.png)"))
                               [:img {:src "/assets/tbl_spacer.png"
                                      :style "vertical-align: top; background-color: white;"}]
                               [:img {:src "/assets/tbl_vjoin.png"
                                      :style "vertical-align: top; background-color: white;"}]
                               [:img {:src (get-icon itm)
                                      :class "table-icon"}]]
                              [:a (*get-in itm [0 :attr])]]
                             [:div.col
                              [:div.line-item
                               [:span {:class "flag-item"}
                                "S"]]]
                             (let [card (get-cardinality (*get itm 0))]
                               (if (or (= card "1..1") (= card "1..*"))
                                 [:div.col
                                  [:div.line-item
                                   card]]
                                 [:div.col
                                  [:div.line-item {:style "opacity: 0.4"}
                                   card]]))
                             [:div.col
                              [:div.line-item {:style "opacity: 0.4"}
                               (*get-in itm [0 :type])]]
                             [:div.col
                              [:div.line-item
                               [:a (*get-in itm [0 :desc])]]]]))
                     (get-profile-attrs profile))))

         (psd/into-file "./resources/" 
                        [:html [:style (pss/style (pss/set-page-style pss/profile-style pss/navigation-menu-style))]
                         (vec (concat
                               [:div.table
                                [:div.row {:style "border: 1px #F0F0F0 solid;
                                  font-size: 11px;
                                  font-family: verdana;
                                  vertical-align: top;"}
                                 [:div.col
                                  [:div.th
                                   [:a "Имя"]]]
                                 [:div.col
                                  [:div.th
                                   [:a "Флаги"]]]
                                 [:div.col
                                  [:div.th
                                   [:a "Кард."]]]
                                 [:div.col
                                  [:div.th
                                   [:a "Тип"]]]
                                 [:div.col
                                  [:div.th
                                   [:a "Описание и ограничения"]]]]
                                [:div.row
                                 [:div.col
                                  [:div.line-item-resource-type
                                   [:img {:src "/assets/icon_element.gif"
                                          :style "vertical-align: top"}]]
                                  "Patient"]
                                 [:div.col
                                  [:div.line-item
                                   ""]]
                                 [:div.col
                                  [:div.line-item {:style "opacity: 0.4"}]
                                  "0..*"]]] (attrs->hc psd/patient-profile)))] "test")


         (defn inner-attrs->hc [attr]
           (letfn [(into-hc [itm]
                     [:tr
                      [:td {:class "line-inner-item"}
                       [:img {:src "/assets/tbl_spacer.png"
                              :style "vertical-align: top"}]
                       [:img {:src "/assets/tbl_vline.png"
                              :style "vertical-align: top; background-color: white"}]
                       [:img {:src "/assets/tbl_vjoin.png"
                              :style "vertical-align: top; background-color: white"}]
                       [:img {:src (get-icon itm)
                              :class "table-icon"}]
                       (*get itm :attr)]
                      [:td {:class "line-item"}
                       [:span {:class "flag-item"}
                        "S"]]
                      (let [card (get-cardinality itm)]
                        (if (or (= card "1..1") (= card "1..*"))
                          [:td {:class "line-item"}
                           card]
                          [:td {:class "line-item"
                                :style "opacity: 0.4"}
                           card]))
                      [:td {:class "line-item"
                            :style "opacity: 0.4"}
                       (*get itm :type)]
                      [:td {:class "line-item"} [:a (*get itm :desc)]]])

                   (into-hc-comp [itm]
                     (let [tr-hc (into-hc itm)]
                       (assoc-in tr-hc (conj (vector-first-path #(= % {:class "line-inner-item"}) tr-hc) :style) "background-image: url(/assets/tbl_bck111.png)")))
                   (add-vline [itm]
                     (insert-into (*get itm 1) 2
                                  [:img {:src "/assets/tbl_vline.png"
                                         :style "vertical-align: top; background-color: white"}]))]

             (map (fn [inner]
                    (if (sequential? inner)
                      (vec (concat (into-hc-comp (first inner)) (inner-attrs->hc inner)))
                      (into-hc inner)))
                  (first (rest attr))))))
