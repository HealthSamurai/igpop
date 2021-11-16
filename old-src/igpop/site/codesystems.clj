(ns igpop.site.codesystems
  (:require [igpop.site.views :as views]
            [igpop.site.utils :as u]))

(defn get-custom-columns [{concept :concepts :as cs}]
  (vec
   (disj (set (mapcat keys concept))
         :system :code :display :definition)))


(defn render-table [columns {concepts :concepts system-common :system :as cs}]
  (conj [:div.table
         (conj [:div.row.th.first-line
                [:div.column
                 [:div.th "Code"]]
                [:div.column
                 [:div.th "Display"]]
                [:div.column
                 [:div.th "Definition"]]]
               (->> columns
                    (map (fn [custom-column]
                           [:div.column
                            [:div.th (name custom-column)]]))))]
        (->> concepts
             (map (fn [{code :code display :display definition :definition :as cpt}]
                    (conj [:div.row
                           [:div.column
                            code]
                           [:div.column
                            display]
                           [:div.column
                            definition]]
                          (->> columns
                               (map (fn [custom-column]
                                      [:div.column
                                       (custom-column cpt)])))))))))

(defn codesystems-to-menu [{codesystems :codesystems :as ctx}]
  (map (fn [itm]
         {:display (name itm) :href (u/href ctx "codesystems" (name itm) {:format "html"})})
       (keys codesystems)))

(defn codesystem-link [nm cs ctx]
  [:a.db-item {:href (u/href ctx "codesystems" (name nm) {:format "html"})}
   [:h5 (name nm)]
   [:div.desc (when (:description cs) (subs (:description cs) 0 (min (count (:description cs)) 55)))]])

(defn codesystem [ctx {{csid :codesystem-id} :route-params :as req}]
  (let [cs (get-in ctx [:codesystems (keyword csid)])
        description (get cs :description)]
    {:status 200
     :body (views/layout ctx
            (views/menu (codesystems-to-menu ctx) req)
            [:div#content [:h1 "CodeSystem " csid]
             [:div.summary description]
             [:hr]
             [:br]
             (render-table (get-custom-columns cs) cs)])}))

(defn codesystems-dashboard [ctx {{cs :resource-type nm :profile} :route-params :as req}]
  {:status 200
   :body (views/layout ctx
                       (into [:div#db-content]
                             (->> (:codesystems ctx)
                                  (sort-by first)
                                  (map (fn [[nm cs]] (codesystem-link nm cs ctx))))))})



(comment

  (def hm (.getAbsolutePath (clojure.java.io/file  "../ig-ae")))

  (require '[igpop.loader])

  (def ctx (igpop.loader/load-project hm))

  (def cs-ids (keys (get-in ctx [:codesystems])))

  (->> (get-in ctx [:codesystems])
       vals
       first
       ;; (render-table [])
       )

  (->> (codesystem ctx {:uri "hh"})
       :body
       )

  ,)

