(ns igpop.dev
  (:require
   [org.httpkit.server :as srv]
   [hiccup.core :as hc]
   [stylo.core :refer [c c?] :as stylo]
   [stylo.rule :refer [join-rules]]
   [igpop.md]
   [clojure.string :as str]
   [clojure.java.io :as io]
   [garden.core]
   [zen.core :as zen]))

(defmulti do-render (fn [ztx request model] (:igpop/view model)))

(defn link [model & [text]]
  [:a {:class (c [:text :red-600] [:px 4])
       :href (str "/m/" (:zen/name model))} (or text (:title model))])

(defn header [ztx ig]
  [:div {:class (c [:p 4] :border-b :shadow-sm :flex)}
   [:div {:class (c [:w 100] [:px 6] :font-bold)}
    [:a {:href "/"} (:title ig)]]
   (->> (:menu ig)
        (mapv (fn [x]
                (if-let [s (zen/get-symbol ztx x)]
                  (link s)
                  [:div "Error:" (pr-str x)])))
        (into [:div {:class (c [:flex] [:space-x 1])}]))])

(def markdown
  (garden.core/css
   [:.md
    [:h1 (join-rules [:font-bold :text-xl])]]))

markdown


(defn layout [ztx request ig cnt]
  (hc/html [:html
            [:head
             [:meta {:charset "utf-8"}]
             [:link {:href "//fonts.googleapis.com/css?family=Montserrat|Roboto&display=swap" :rel "stylesheet"}]
             [:link {:rel "stylesheet"
                     :href "https://cdnjs.cloudflare.com/ajax/libs/github-markdown-css/5.0.0/github-markdown-light.css"
                     :crossorigin "anonymous"
                     :referrerpolicy"no-referrer"}]
             [:style (stylo.core/compile-styles @stylo.core/styles @stylo.core/media-styles)]
             [:title (:title ig)]]
            [:body {:class (c [:p 0] [:m 0] {})}
             (header ztx ig)
             cnt]]))

(defn read-file [ztx file]
  (let [pth (str (first (:paths @ztx)) "/" file)]
    (when (.exists (io/file pth))
      (slurp pth))))

(defmethod do-render 'igpop/index
  [ztx request ig]
  [:div
   [:div {:class (c [:p 4] :mx-auto [:w 230])}
    [:div.markdown-body  (igpop.md/parse-markdown (read-file ztx (:index ig)))]]])

(defmethod do-render 'igpop/section
  [ztx request model]
  [:div {:class (c [:w 200] :mx-auto [:py 4])}
   (when-let [index (:index model)]
     [:div {:class (c [:p 4] :mx-auto [:w 230])}
      [:div.markdown-body
       (igpop.md/parse-markdown (read-file ztx index))]])
   [:pre (pr-str model)]])

(defmethod do-render :default
  [ztx request model]
  [:div "Error no render for " [:pre (pr-str model)]])

(defn dispatch [ztx request]
  (let [ig (zen/get-symbol ztx (:entry @ztx))]
    {:status 200
     :headers {"content-type" "text/html"}
     :body (layout ztx
                   request
                   ig
                   (let [uri (:uri request)]
                     (if (str/starts-with? uri "/m/")
                       (let [sym (subs uri 3)
                             model (zen/get-symbol ztx (symbol sym))]
                         (if model
                           (do-render ztx request model)
                           (layout ztx
                                   request
                                   [:div "Error" sym])))
                       (let [ig (zen/get-symbol ztx (:entry @ztx))]
                         (do-render ztx request ig)))))}))

(defn start [ztx opts]
  (when-let [srv  (:web/srv ztx)]
    (println "Stop srv")
    (srv))
  (let [srv (srv/run-server #(dispatch ztx %) {:port 8899})]
    (swap! ztx assoc :web/srv srv))
  (when-let [e (:entry opts)]
    (zen/read-ns ztx (symbol (namespace e))))
  :started)

(comment

  (def ztx (zen/new-context {:entry 'fhir-ru/ig
                             :paths ["/Users/niquola/fhir-ru/zrc"]}))

  (zen/read-ns ztx 'fhir-ru)

  (zen/get-symbol ztx 'fhir-ru/ig)

  (start ztx {:entry 'fhir-ru/ig})
  (:paths @ztx)


  )
