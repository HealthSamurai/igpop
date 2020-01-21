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
  (map
   (fn [[doc-id doc]]
     {:display (or (:title doc) (name doc-id)) :href (u/href ctx "docs" (name doc-id) {:format "html"})}) pages))

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

(defn docs-link [nm doc ctx]
  [:a.db-item {:href (u/href ctx "docs" (name nm) {:format "html"})}
   [:h5 (name nm)]
   [:div.desc (when (:title doc) (subs (:title doc) 0 (min (count (:title doc)) 55)))]])

(defn dashboard [ctx req]
  {:status 200
   :body (views/layout
          ctx
          style-tag
          ;; (views/menu (docs-to-menu ctx) req)
          ;; [:div#content]
          (into [:div#db-content]
                (->> (get-in ctx [:docs :pages])
                     (sort-by first)
                     (mapv (fn [[doc-id doc]]
                             (docs-link doc-id doc ctx))))))})

(defn doc-page [ctx {{doc-id :doc-id} :route-params :as req}]
  (let [doc (get-in ctx [:docs :pages (keyword doc-id)])]
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
