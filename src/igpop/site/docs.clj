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
  [:body])

(def style-tag [:style (gc/css styles)])

(defn dashboard [ctx req]
  {:status 200
   :body (views/layout
          ctx
          style-tag
          (views/menu (docs-to-menu ctx) req)
          [:div#content]
          #_(into [:div#content]
                (->> (get-in ctx [:docs :pages])
                     (sort-by first)
                     (mapv (fn [[doc-id doc]]
                             [:pre (pr-str doc-id doc)])))))})

(defn doc-page [ctx {{doc-id :doc-id} :route-params :as req}]
  (let [doc (get-in ctx [:docs :pages (keyword doc-id)])]
    {:status 200
     :body (views/layout
            ctx
            style-tag
            (views/menu (docs-to-menu ctx) req)
            [:div#content
             [:pre (pr-str (dissoc doc :source))]
             (markdown.core/md-to-html-string (:source doc))])}))
