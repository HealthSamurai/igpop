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

(defn valuesets-to-menu [{valuesets :valuesets}]
  (map (fn [itm]
         {:display (name itm) :href (str "/valuesets/" (name itm))})
       (keys valuesets)))

(defn menu' [itms {uri :uri}]
  (letfn [(current-page [uri res-url] (= uri res-url))]
    [:div#main-menu
     (for [{display :display href :href items :items} (sort-by first itms)]
       [:div
        [:a {:href href :class (when (current-page uri href) "active")} display]
        (when (> (count items) 0)
          (into [:section] (for [{display :display href :href} items]
                             [:a {:href href :class (when (current-page uri href) "active")} display])))])]))

(defn valueset-link [nm vs]
  [:a.db-item {:href (str "/valuesets/" (name nm))}
   [:h5 (name nm)]
   [:div.desc (when (:description vs) (subs (:description vs) 0 (min (count (:description vs)) 55)))]])

(defn valueset [ctx {{vid :valuset-id} :route-params :as req}]
  (let [vs (get-in ctx [:valuesets (keyword vid)])
        description (get vs :description)]
    {:status 200
     :body (views/layout ctx
            (menu' (valuesets-to-menu ctx) req)
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
                                  (map (fn [[nm vs]] (valueset-link nm vs))))))})
