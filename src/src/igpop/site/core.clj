(ns igpop.site.core
  (:require
   [org.httpkit.server]
   [route-map.core]
   [igpop.site.views :as views]
   [clojure.string :as str]
   [clj-yaml.core]
   [clojure.java.io :as io]
   [igpop.site.profiles]
   [igpop.site.valuesets :as valuesets]))

(def ig-path "../us-core")

(defn read-yaml [pth]
  (clj-yaml.core/parse-string
   (slurp pth)))

(defn read-profiles [pth]
  (let [manifest (read-yaml (str pth "/ig.yaml"))
        files (file-seq (io/file (str pth "/src")))]
    (->> files
         (reduce
          (fn [acc f]
            (let [nm (.getName f)]
              (if (str/starts-with? nm "vs.")
                (let [rt (str/replace nm #"\.yaml$" "")]
                  (assoc-in acc [:valuesets (keyword rt)]
                            (read-yaml (.getPath f))))
                (if (and (str/ends-with? nm ".yaml"))
                  (let [rt (str/replace nm #"\.yaml$" "")]
                    (println "Load.." rt)
                    (assoc-in acc [:profiles (keyword rt) :basic]
                              (read-yaml (.getPath f))))
                  (do
                    (println "TODO" nm)
                    acc))))) {}))))

(comment

  (get-in (read-profiles ig-path) [:valuesets :vs.patient-identifiers])

  (ig)

  )

(defn ig []
  (read-profiles ig-path)
  )

(defn welcome [ctx req]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (views/layout
          [:div#header
           [:h5 "FHIR-RU Core"]]
          (menu ctx req)
          [:div#content
           [:h1 "Hello"]])})


(defn valuesets-dashboard [ctx req]
  {:status 200
   :body (views/layout
          [:div#main-menu
           [:a {:href "/valuesets/patient-identity"} "patient-identity"]]
          [:div#content
           [:h1 "Valuesets"]])})

(comment (defn valueset [ctx {{vid :valuset-id} :route-params :as req}]
           {:status 200
            :body (views/layout
                   [:div#main-menu
                    [:a {:href "/valuesets/patient-identity"} "patient-identity"]]
                   [:div#content [:h1 "Valueset " vid]])}))

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

(def routes
  {:GET #'welcome
   "valuesets" {:GET #'valuesets-dashboard
                [:valuset-id] {:GET #'valueset}}
   "profiles" {:GET #'igpop.site.profiles/profiles-dashboard
               [:resource-type] {:GET #'igpop.site.profiles/profile
                                 [:profile] {:GET #'igpop.site.profiles/profile}}}})

(defn handler [{uri :uri meth :request-method :as req}]
  (if-let [{handler :match params :params} (route-map.core/match [meth uri] #'routes)]
    (handler (ig) (assoc req :route-params params))
    {:status 200 :body "Ok"}))

(defn start [port]
  (org.httpkit.server/run-server #'handler {:port port}))

(comment
  (def srv (start 8899))

  (srv)

  (handler {:uri "/" :request-method :get})

  )
