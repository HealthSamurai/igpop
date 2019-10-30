(ns igpop.site.core
  (:require
   [clojure.string :as str]
   [igpop.loader]
   [igpop.site.profiles]
   [igpop.site.valuesets :as valuesets]
   [igpop.site.views :as views]
   [org.httpkit.server]
   [ring.middleware.head]
   [ring.util.codec]
   [ring.util.response]
   [route-map.core]))

(defn welcome [ctx req]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (views/layout
          [:div#header
           [:h5 "FHIR-RU Core"]]
          [:div#content
           [:h1 "Hello"]])})


(defn valuesets-dashboard [ctx req]
  {:status 200
   :body (views/layout
          [:div#main-menu
           [:a {:href "/valuesets/patient-identity"} "patient-identity"]]
          [:div#content
           [:h1 "Valuesets"]])})


(defn valueset [ctx {{vid :valuset-id} :route-params :as req}]
  (let [vs (get-in ctx [:valuesets (-> "vs."
                                       (str vid)
                                       keyword)])
        description (get vs :description)]
    {:status 200
     :body (views/layout
            [:div#main-menu
             [:a {:href "/valuesets/patient-identity"} "patient-identity"]]
            [:div#content [:h1 "Valueset " vid]
             [:div.summary description]
             [:hr]
             [:br]
             (valuesets/render-tb-vs vs)])}))


(defn handle-static [h {meth :request-method uri :uri :as req}]
  (if (and (#{:get :head} meth)
           (or (str/starts-with? (or uri "") "/static/")
               (str/starts-with? (or uri "") "/favicon.ico")))
    (let [opts {:root "public"
                :index-files? true
                :allow-symlinks? true}
          path (subs (ring.util.codec/url-decode (:uri req)) 8)]
      (-> (ring.util.response/resource-response path opts)
          (ring.middleware.head/head-response req)))
    (h req)))

(defn wrap-static [h]
  (fn [req]
    (handle-static h req)))

(def routes
  {:GET #'welcome
   "valuesets" {:GET #'valuesets-dashboard
                [:valuset-id] {:GET #'valueset}}
   "profiles" {:GET #'igpop.site.profiles/profiles-dashboard
               [:resource-type] {:GET #'igpop.site.profiles/profile
                                 [:profile] {:GET #'igpop.site.profiles/profile}}}})

(defn dispatch [{uri :uri meth :request-method :as req}]
  (if-let [{handler :match params :params} (route-map.core/match [meth uri] #'routes)]
    (handler (ig) (assoc req :route-params params))
    {:status 200 :body "Ok"}))

(def handler (wrap-static #'dispatch))

(defn start [home port]
  (org.httpkit.server/run-server #'handler {:port port}))

(comment
  (def srv (start 8899))

  (srv)
  srv

  (handler {:uri "/" :request-method :get})

  )
