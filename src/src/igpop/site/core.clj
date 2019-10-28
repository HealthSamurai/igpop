(ns igpop.site.core
  (:require
   [org.httpkit.server]
   [route-map.core]
   [igpop.site.views :as views]
   [clojure.string :as str]
   [clj-yaml.core]
   [clojure.java.io :as io]))

(def ig-path "/Users/niquola/igpop/us-core")

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
              (if (and (str/ends-with? nm ".yaml"))
                (let [rt (str/replace nm #"\.yaml$" "")]
                  (assoc-in acc [:profiles (keyword rt) :basic]
                            (read-yaml (.getPath f))))
                (println "TODO" nm)))) {})
         )
    ))

(comment

  (read-profiles ig-path)

  )

(defn ig []
  (read-profiles ig-path)
  )

(defn menu [ctx]
  [:div#main-menu
   (for [[rt profiles] (->> (:profiles ctx)
                            (sort-by first))]
     [:div
      (if (and (= 1 (count profiles))
               (= :basic (first (keys profiles))))
        [:a {:href (str "/profiles/" (name rt) "/basic")} (name rt)]
        [:div
         [:a (name rt)]
         (into [:section]
               (for [[nm pr] profiles]
                 [:a {:href (str "/profiles/" (name rt) "/" (name nm))} (name nm)]))])])])

(defn welcome [ctx req]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (views/layout
          [:div#header
           [:h5 "FHIR-RU Core"]]
          (menu ctx)
          [:div#content
           [:h1 "Hello"]])})

(defn render-row [is-lst? pth nm el]
  [:tr
   [:td.tree
    (loop [res [:span]
           [p & ps] pth]
      (if (nil? p)
        res
        (recur
         (conj res [:div {:class (str p (when (and is-lst? (empty? ps)) " last"))}])
         ps)))
    [:div.conn]
    [:snan.nm
     (if-let [tp (:type el)]
       [:span.tp {:class tp}
        (subs tp 0 1)]
       [:span.tp {:class "obj"} "{}"])
     nm
     (when (:required el) [:span.required "*"])]]
   #_[:td.desc
    [:div (or (:description el) "&nbsp;")]
    (when-let [tp (:type el)]
      [:a.tp-link {:href "/"} tp])
    ]
   [:td (when-let [tp (:type el)]
          [:a.tp-link {:href "/"} tp])]
   [:td.desc
    (or (:description el) "&nbsp;")
    ]])

(defn elements [rows pth els]
  (loop [[[nm el] & es] els
         rows rows]
    (if (nil? el)
      rows
      (let [is-lst? (empty? es)
            tr (render-row is-lst? pth nm el)
            rows' (conj rows tr)
            rows'' (if-let [els' (if (= :extension nm) el (:elements el))]
                     (elements rows'
                               (if is-lst?
                                 (if (empty? pth)
                                   (conj pth "sps")
                                   (conj (into [] (butlast pth)) "lsps" "sps"))
                                 (conj pth "sps"))
                               els')
                    rows')]
        (recur es rows'')))))

(defn profile [ctx {{rt :resource-type nm :profile} :route-params}]
  (let [profile (get-in ctx [:profiles (keyword rt) (keyword nm)])]
    {:status 200
     :body (views/layout
            [:div#header
             [:h5 "FHIR US-Core"]
             [:div#top-nav
              [:a {:href "/"} "Docs"]
              [:a {:href "/profiles"} "Profiles"]
              [:a {:href "/valuesets"} "ValueSets"]]]

            (menu ctx)
            [:div#content
             [:h1 rt " " [:span.sub (str/lower-case rt) "-" nm]]
             [:div.summary (:description profile)]
             [:hr]

             [:br]
             [:h3 "Profile"]
             (let [rows (elements [] [] (:elements profile))]
               [:table.prof
                (into [:tbody] rows)])

             [:br]
             [:h3 "API"]
             [:br]
             [:h3 "Examples"]
             ])}))

(def routes
  {:GET #'welcome
   "profiles" {[:resource-type] {:GET #'profile
                                 [:profile] {:GET #'profile}}}})

(defn handler [{uri :uri meth :request-method :as req}]
  (if-let [{handler :match params :params} (route-map.core/match [meth uri] #'routes)]
    (handler (ig) (assoc req :route-params params))
    {:status 200 :body "Ok"}))

(defn start [port]
  (org.httpkit.server/run-server #'handler {:port port}))

(comment
  (def srv (start 8899))

  (handler {:uri "/" :request-method :get})

  )







