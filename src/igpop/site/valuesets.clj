(ns igpop.site.valuesets
  (:require [igpop.site.views :as views]
            [igpop.site.utils :as u]))

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
           [:div.th "Definition"]]]] (map (fn [{code :code display :display definition :definition :as cpt}]
                                    [:div.row
                                     [:div.column
                                      code]
                                     [:div.column
                                      system]
                                     [:div.column
                                      display]
                                     [:div.column
                                      definition]]) concepts)))

(defn valuesets-to-menu [{valuesets :valuesets :as ctx}]
  (map (fn [itm]
         {:display (name itm) :href (u/href ctx "valuesets" (name itm) {:format "html"})})
       (keys valuesets)))

(defn valueset-link [nm vs ctx]
  [:a.db-item {:href (u/href ctx "valuesets" (name nm) {:format "html"})}
   [:h5 (name nm)]
   [:div.desc (when (:description vs) (subs (:description vs) 0 (min (count (:description vs)) 55)))]])

(defn valueset [ctx {{vid :valuset-id} :route-params :as req}]
  (let [vs (get-in ctx [:valuesets (keyword vid)])
        description (get vs :description)]
    {:status 200
     :body (views/layout ctx
            (views/menu (valuesets-to-menu ctx) req)
            [:div#content [:h1 "Valueset " vid]
             [:div.summary description]
             [:hr]
             [:br]
             (render-table vs)])}))

(defn valuesets-dashboard [ctx {{vs :resource-type nm :profile} :route-params :as req}]
  {:status 200
   :body (views/layout ctx
                       (into [:div#db-content]
                             (->> (:valuesets ctx)
                                  (sort-by first)
                                  (map (fn [[nm vs]] (valueset-link nm vs ctx))))))})
