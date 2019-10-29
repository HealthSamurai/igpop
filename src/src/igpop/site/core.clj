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
              (println nm)
              (if (str/starts-with? nm "vs.")
                acc
                (if (and (str/ends-with? nm ".yaml"))
                  (let [rt (str/replace nm #"\.yaml$" "")]
                    (println "Load.." rt)
                    (assoc-in acc [:profiles (keyword rt) :basic]
                              (read-yaml (.getPath f))))
                  (do
                    (println "TODO" nm)
                    acc))))) {}))))

(comment

  (read-profiles ig-path)

  (ig)

  )



(defn ig []
  (read-profiles ig-path)
  )

(defn current-page [uri res-url]
  (= uri res-url))

(defn menu [ctx {uri :uri}]
  [:div#main-menu
   (for [[rt profiles] (->> (:profiles ctx)
                            (sort-by first))]
     [:div
      (if (and (= 1 (count profiles))
               (= :basic (first (keys profiles))))
        (let [res-url (str "/profiles/" (name rt) "/basic")]
          [:a {:href res-url :class (when (current-page uri res-url) "active")} (name rt)])
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
          (menu ctx req)
          [:div#content
           [:h1 "Hello"]])})

(def type-symbols
  {"Reference" "⮕"
   "date" "⧗"
   "dateTime" "⧗"
   "Period" "⧗"
   "Address" "⌂"
   "HumanName" "웃"

   }


  )

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
       [:span.tp {:class (str tp (when (Character/isUpperCase (first tp)) " complex"))}
        (or (get type-symbols tp) (subs tp 0 1))]
       [:span.tp {:class "obj"} "/"])
     nm
     (when (:required el) [:span.required "*"])]
    " "
    (when-let [tp (:type el)]
      [:a.tp-link {:href "/"} tp])
    (when (:collection el) [:span.tp-link.coll " [0..*]"])
    ]
   #_[:td 
    
    ]
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

(defn enrich [base profile]
  (if-let [els (:elements profile)]
    (let [els' (reduce (fn [acc [k v]]
                         (if-let [base-element (get-in base [:elements k])]
                           (assoc acc k (enrich base-element v))
                           acc)) els els)]
      (assoc (merge (dissoc base :elements) profile) :elements els'))
    (merge (dissoc base :elements) profile)))

(defn top-nav []
  [:div#header
   [:h5 "FHIR RU Core"]
   [:div#top-nav
    [:a {:href "/"} "Docs"]
    [:a {:href "/profiles"} "Profiles"]
    [:a {:href "/valuesets"} "ValueSets"]]])

(defn profile [ctx {{rt :resource-type nm :profile} :route-params :as req}]
  (let [profile (get-in ctx [:profiles (keyword rt) (keyword nm)])
        base (read-yaml (str "/Users/niquola/igpop/igpop-fhir-4.0.0/" rt ".yaml"))
        profile (enrich base profile)]
    {:status 200
     :body (views/layout
            (top-nav)
            (menu ctx req)
            [:div#content
             [:h1 rt " " [:span.sub (str/lower-case rt) "-" nm]]
             [:div.summary (:description profile)]
             [:hr]

             [:br]
             [:br]
             [:h5 [:div.tp.profile.complex "Pr"] rt]
             (let [rows (elements [] [] (:elements profile))]
               [:table.prof
                (into [:tbody] rows)])

             [:br]
             [:h3 "Examples"]
             [:br]
             [:h3 "API"]
             ])}))

(defn profiles-dashboard [ctx {{rt :resource-type nm :profile} :route-params :as req}]
  {:status 200
   :body (views/layout
          (top-nav)
          (menu ctx req)
          [:div#content [:h1 "Profiles"]])}
  )

(defn valuesets-dashboard [ctx req]
  {:status 200
   :body (views/layout
          (top-nav)
          [:div#main-menu
           [:a {:href "/valuesets/patient-identity"} "patient-identity"]]
          [:div#content
           [:h1 "Valuesets"]])}
  )

(defn valueset [ctx {{vid :valuset-id} :route-params :as req}]
  {:status 200
   :body (views/layout
          (top-nav)
          [:div#main-menu
           [:a {:href "/valuesets/patient-identity"} "patient-identity"]]
          [:div#content [:h1 "Valueset" vid]])}
  )

(def routes
  {:GET #'welcome
   "valuesets" {:GET #'valuesets-dashboard
                [:valuset-id] {:GET #'valueset}}
   "profiles" {:GET #'profiles-dashboard
               [:resource-type] {:GET #'profile
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







