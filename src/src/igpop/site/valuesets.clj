(ns igpop.site.valuesets
  (:require [igpop.site.views :as views]))

(defn render-table [{concepts :concepts system :system :as vs}]
  (conj [:div.table
         [:div.row.th.first-line
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

(defn menu-vs [ctx {uri :uri}]
  (letfn [(current-page [uri res-url] (= uri res-url))]
    [:div#main-menu
     (for [vs (keys (:valuesets ctx))] 
       [:div
        (let [res-url (str "/valuesets/" (name vs))]
          [:a {:href res-url :class (when (current-page uri res-url) "active")} (name vs)])])]))

(defn valuesets-dashboard [ctx req]
  {:status 200
   :body (views/layout
          (menu-vs ctx req)
          [:div#content
           [:h1 "Valuesets"]])})

(defn valueset [ctx {{vid :valuset-id} :route-params :as req}]
  (let [vs (get-in ctx [:valuesets (keyword vid)])
        description (get vs :description)]
    {:status 200
     :body (views/layout
            (menu-vs ctx req)
            [:div#content [:h1 "Valueset " vid]
             [:div.summary description]
             [:hr]
             [:br]
             (render-table vs)])}))
