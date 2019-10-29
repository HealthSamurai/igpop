(ns igpop.site.valuesets
  (:require [igpop.site.core :refer :all]
            [igpop.site.views :as views]))

(defn render-tb-vs [{concepts :concepts system :system :as vs}]
  (conj [:div.table
         [:div.row.th
          [:div.column
           [:div.th "Code"]]
          [:div.column
           [:div.th "System"]]
          [:div.column
           [:div.th "Display"]]
          [:div.column
           [:div.th "Definition"]]]] (map (fn [{code :code display :display :as cpt}]
                                    [:div.row
                                     [:div.column
                                      code]
                                     [:div.column
                                      system]
                                     [:div.column
                                      display]
                                     [:div.column
                                      "???"]]) concepts)))

(defn vs [ctx {{vid :valuset-id} :route-params :as req}]
  (let [vs (get-in ctx [:valuesets (-> "vs."
                                       (str vid)
                                       keyword)])
        description (get vs :description)]
    {:status 200
     :body (views/layout
            (top-nav)
            [:div#main-menu
             [:a {:href "/valuesets/patient-identity"} "patient-identity"]]
            [:div#content [:h1 "Valueset " vid]
             [:div.summary description]
             [:hr]
             [:br]
             (render-tb-vs vs)])}))

(comment
  (render-tb-vs (get-in (read-profiles ig-path) [:valuesets :vs.patient-identifiers]))



  )
