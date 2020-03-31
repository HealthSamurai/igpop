(ns igpop.site.docs
  (:require
   [igpop.site.views :as views]
   [clojure.string :as str]
   [garden.core :as gc]
   [markdown.core :as md]
   [igpop.site.utils :as u]))

(defn current-page [uri res-url]
  (= uri res-url))

(comment (defn menu [ctx {uri :uri}]
          [:div#main-menu
           (for [[doc-id doc] (:pages (:docs ctx))]
             (let [res-url (str "/docs/" (name doc-id))]
               [:a {:href res-url :class (when (current-page uri res-url) "active")} (or (:title doc) doc-id)]))]))

(defn docs-to-menu [{{pages :pages} :docs :as ctx}]
  (mapv
   (fn [[doc-id doc]]
     {:display (or (:title (:basic doc)) (name doc-id))
      :href (if (contains? doc :basic)
              (u/href ctx "docs" (name doc-id) "basic" {:format "html"})
              "javascript:void(0)")
      :items (->> (keys doc)
                  (filter #(not (= :basic %)))
                  (map (fn [n]
                         {:display (or (:title (n doc)) (name n))
                            :href (u/href ctx "docs" (name doc-id) (name n) {:format "html"})}))
                  (sort-by :display))}) pages))

(def styles
  [[:.content :thead
    {:color "#5b6975"
     :border-bottom "2px solid #E6ECF1"}]
   [:.content :thead :tr :th
    {:padding "8px"}]
   [:.content :tbody :tr
    {:border-bottom "2px solid #E6ECF1"}]
   [:.content :tbody :td
    {:padding "8px"}]
   [:.content :table
    {:margin "20px 0px"}]])

(def style-tag [:style (gc/css styles)])

(defn docs-link [doc-id cur doc ctx]
  [:a.db-item {:href (u/href ctx "docs" (name doc-id) (name cur) {:format "html"})}
   [:h5 (name doc-id) ":" (name cur)]
   [:div.desc (when (:title doc) (subs (:title doc) 0 (min (count (:title doc)) 55)))]])

(defn dashboard [ctx {{doc-id :doc-id cur :curr-doc} :route-params :as req}]
  {:status 200
   :body (views/layout
          ctx
          style-tag
          ;; (views/menu (docs-to-menu ctx) req)
          ;; [:div#content]
          (into [:div#db-content]
                (->> (get-in ctx [:docs :pages])
                     (sort-by first)
                     (mapcat (fn [[doc-id doc]]
                             (->> doc
                                  (mapv (fn [[cur dc]] (docs-link doc-id cur dc ctx)))))))))})

(defn doc-page [ctx {{curr-doc :curr-doc} :route-params {doc-id :doc-id} :route-params :as req}]
  (let [doc (get-in ctx [:docs :pages (keyword doc-id) (keyword curr-doc)])]
    {:status 200
     :body (views/layout
            ctx
            style-tag
            (views/menu (docs-to-menu ctx) req)
            [:div#content
             [:pre ""]
             (markdown.core/md-to-html-string (:source doc))])}))

(defn home-page [ctx]
  (let [doc (get-in ctx [:docs :home :homepage])]
     [:div#content
      [:pre ""]
      (markdown.core/md-to-html-string (:source doc))]))
