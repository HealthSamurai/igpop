(ns igpop.site.valuesets
  (:require [igpop.site.views :as views]
            [igpop.site.utils :as u]))

(defn get-custom-columns [{concept :concepts :as vs}]
  (vec
   (disj
    (set
     (->> concept
          (reduce (fn [coll item]
                    (concat coll
                            (->> item
                                 (reduce-kv (fn [s k v]
                                              (conj s k)) #{})
                                 ))) []))) :system :code :display :definition)))

(defn render-table [columns {concepts :concepts system-common :system :as vs}]
  (conj [:div.table
         (conj [:div.row.th.first-line
                [:div.column
                 [:div.th "Code"]]
                [:div.column
                 [:div.th "System"]]
                [:div.column
                 [:div.th "Display"]]
                [:div.column
                 [:div.th "Definition"]]]
               (->> columns
                    (map (fn [custom-column]
                           [:div.column
                            [:div.th (name custom-column)]]))))]

        (->> concepts
             (map (fn [{code :code system :system display :display definition :definition :as cpt}]
                    (conj [:div.row
                           [:div.column
                            code]
                           [:div.column
                            (if system
                              system
                              system-common)]
                           [:div.column
                            display]
                           [:div.column
                            definition]]
                          (->> columns
                               (map (fn [custom-column]
                                      [:div.column
                                       (custom-column cpt)])))))))))

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
             (render-table (get-custom-columns vs) vs)])}))

(defn valuesets-dashboard [ctx {{vs :resource-type nm :profile} :route-params :as req}]
  {:status 200
   :body (views/layout ctx
                       (into [:div#db-content]
                             (->> (:valuesets ctx)
                                  (sort-by first)
                                  (map (fn [[nm vs]] (valueset-link nm vs ctx))))))})
