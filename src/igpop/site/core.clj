(ns igpop.site.core
  (:require
   [clojure.string :as str]
   [igpop.loader]
   [igpop.site.profiles]
   [igpop.site.valuesets]
   [igpop.site.docs]
   [igpop.site.views :as views]
   [org.httpkit.server]
   [ring.middleware.head]
   [ring.util.codec]
   [ring.util.response]
   [route-map.core]
   [igpop.site.utils :as u]
   [org.httpkit.server :as http]
   [clojure.java.io :as io]
   [json-rpc.core]))

(defn welcome [ctx req]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (views/layout
          ctx
          (igpop.site.docs/home-page ctx))})

(defn handle-static [{meth :request-method uri :uri :as req}]
  (when (and (#{:get :head} meth)
           (or (str/starts-with? (or uri "") "/static/")
               (str/starts-with? (or uri "") "/favicon.ico")))
    (let [opts {:root "public"
                :index-files? true
                :allow-symlinks? true}
          path (subs (ring.util.codec/url-decode (:uri req)) 8)]
      (-> (ring.util.response/resource-response path opts)
          (ring.middleware.head/head-response req)))))

(defn source [ctx req]
  {:status 200
   :body (clj-yaml.core/generate-string (dissoc ctx :fhir))})

(defn lsp [ctx req]
  (http/with-channel
    req chann
    (http/on-close chann (fn [status] (println "chann closed: " status)))
    (http/on-receive chann (fn [data]
                             (let [msg (cheshire.core/parse-string data keyword)
                                   resp (try
                                          (cond-> (json-rpc.core/proc {:manifest ctx :channel chann} msg)
                                            (:id msg) (assoc :id (:id msg)))
                                          (catch Exception err
                                            (println "ERROR:" err)
                                            {:error {:code -32603
                                                     :message (.getMessage err)}}))]
                               (println "IN:" msg)
                               (println "RESP:" resp)
                               (when (:id msg)
                                 (http/send! chann (cheshire.core/generate-string resp))))))))

(defn get-profile [ctx req]
(let [parsed-name (-> req
                        (get :uri)
                        (clojure.string/replace #"/get-profile/" "")
                        (clojure.string/split #"&"))
        file-name-yaml (if (= "basic" (last parsed-name))
                         (str (first parsed-name) ".yaml")
                         (str (clojure.string/join "/" parsed-name) ".yaml"))
        file-name-igpop (if (= "basic" (last parsed-name))
                          (str (first parsed-name) ".igpop")
                          (str (clojure.string/join "/" parsed-name) ".igpop"))]
    (cond
      (.exists (io/file (str (:home ctx) "/src/" file-name-yaml)))
      (let [content (slurp (io/file (str (:home ctx) "/src/" file-name-yaml)))]
        {:status 200
         :body content})
      (.exists (io/file (str (:home ctx) "/src/" file-name-igpop)))
      (let [content (slurp (io/file (str (:home ctx) "/src/" file-name-igpop)))]
        {:status 200
         :body content})
      :else {:status 404 :body "File not found!"})))



(defn edit [ctx req]
  (println req)
  {:status 200
   :headers {}
   :body (io/input-stream (io/resource "public/editor/index.html"))})

(defn post-profile! [ctx req]
  (let [parsed-name (-> req
                        (get :uri)
                        (clojure.string/replace #"/post-profile/" "")
                        (clojure.string/split #"&"))
        file-name-yaml (if (= "basic" (last parsed-name))
                         (str (first parsed-name) ".yaml")
                         (str (clojure.string/join "/" parsed-name) ".yaml"))
        file-name-igpop (if (= "basic" (last parsed-name))
                          (str (first parsed-name) ".igpop")
                          (str (clojure.string/join "/" parsed-name) ".igpop"))]
    (cond
      (.exists (io/file (str (:home ctx) "/src/" file-name-yaml)))
      (let [file (io/file (str (:home ctx) "/src/" file-name-yaml))
            content (slurp (get req :body))]
        (println "content" content)
        (spit file content)
        {:status 200
         :body "File has been saved!"})
      (.exists (io/file (str (:home ctx) "/src/" file-name-igpop)))
      (let [file (io/file (str (:home ctx) "/src/" file-name-igpop))
            content (slurp (get req :body))]
        (spit file content)
        {:status 200
         :body "File has been saved!"}))))

(def routes
  {:GET #'welcome
   "ig.yaml" {:GET #'source}
   "lsp" {:GET #'lsp}
   "docs" {:GET #'igpop.site.docs/dashboard
          [:doc-id] {[:curr-doc] {:GET #'igpop.site.docs/doc-page}}}
   "valuesets" {:GET #'igpop.site.valuesets/valuesets-dashboard
                [:valuset-id] {:GET #'igpop.site.valuesets/valueset}}
   "get-profile" {[:profile-id] {:GET #'get-profile}}
   "post-profile" {[:profile-id] {:POST #'post-profile!}}
   "edit" {[:profile-id] {:GET #'edit}}
   "profiles" {:GET #'igpop.site.profiles/profiles-dashboard
               [:resource-type] {:GET #'igpop.site.profiles/profile
                                 [:profile] {:GET #'igpop.site.profiles/profile}}}})

(defn *dispatch [ctx {uri :uri meth :request-method :as req}]
  (let [uri (str/replace uri #"\.html$" "")
        req (assoc req :uri uri)]
    (if-let [{handler :match params :params} (route-map.core/match [meth uri] #'routes)]
      (handler ctx (assoc req :route-params params))
      {:status 200 :body "Ok"})))

(defn dispatch [ctx {uri :uri meth :request-method :as req}]
  (or
   (handle-static req)
   (do
     (igpop.loader/reload ctx)
     (*dispatch @ctx req))))

(defn mk-handler [home]
  (let [ctx (atom (igpop.loader/load-project home))]
    (fn [req]
      (dispatch ctx req))))

(defn start [home port]
  (let [h (mk-handler home)]
    (println "Run server on http://localhost:" port)
    (org.httpkit.server/run-server h {:port port})))

(defn dump-page [ctx home pth & [idx]]
  (let [href (apply u/href {} pth)
        {body :body} (*dispatch ctx {:request-method :get :uri href})
        [pth opts] (if (map? (last pth)) [(butlast pth) (last pth)] [pth {}])
        output (apply io/file (into [home "build"]
                                    (if idx
                                      (into pth ["index.html"])
                                      (if-let [fmt (:format opts)]
                                        (into  (vec (butlast pth))
                                               [(str (last pth) "." fmt)])
                                        pth))))]
    (println "Build.." href " => " (.getPath output))
    (.mkdir (apply io/file (into [home "build"] (butlast pth))))
    (spit (.getPath output) body)))

(defmacro get-static []
  (let [r (clojure.string/join " "  (reduce (fn [acc f]
                                              (if (.isDirectory f)
                                                (into acc (map (fn [el]
                                                                 (str (.getName f) "/" (.getName el)))
                                                               (filter #(not (.isDirectory %)) (file-seq f))))
                                                (if (not (some
                                                          (fn [el]
                                                            ;;(println el)
                                                            (re-find (re-pattern (.getName f)) el))
                                                          acc))
                                                  (conj acc (.getName f))
                                                  acc)))
                                            []
                                            (->> (str (System/getProperty "user.dir") "/resources" "/public")
                                                 clojure.java.io/file
                                                 file-seq
                                                 (filter #(not (= (.getName %) "public"))))))]
    `~r))

(defn build [home base-url]
  (let [ctx (-> (igpop.loader/load-project home)
                (assoc :base-url base-url)
                (assoc-in [:flags :no-edit] true))]
    (.mkdir (io/file home "build"))
    (.mkdir (io/file home "build" "static"))
    (.mkdir (io/file home "build" "profiles"))

    (dump-page ctx home [] :index)
    (dump-page ctx home ["profiles"] :index)
    (doseq [[rt prs] (:profiles ctx)]
      (doseq [[id pr] (if-not (some #(= % :basic) (keys prs))
                (assoc prs :basic {})
                prs)]
        (dump-page ctx home ["profiles" (name rt) (name id) {:format "html"}])))

    (.mkdir (io/file home "build" "valuesets"))
    (dump-page ctx home ["valuesets"] :index)
    (doseq [[id _] (get-in ctx [:valuesets])]
      (dump-page ctx home ["valuesets" (name id) {:format "html"}]))

    (.mkdir (io/file home "build" "docs"))
    (dump-page ctx home ["docs"] :index)
    (doseq [[doc-id doc] (get-in ctx [:docs :pages])]
      (doseq [[cur _] (if-not (some #(= % :basic) (keys doc))
                         (assoc doc :basic {})
                         doc)]
        (dump-page ctx home ["docs" (name doc-id) (name cur) {:format "html"}])))

    (doseq [f (str/split (get-static) #" ")]
      (when-not (or (= f "static-resources") (= f "static-resources\n"))
        (io/copy (io/input-stream (io/resource (str "public/" f))) (io/file home "build" "static" (last (str/split f #"/"))))))
    ))

(comment

  (def hm (.getAbsolutePath (io/file  "example")))

  (def srv (start hm 8899))

  (build hm "http://localhost/igpop/example/build")
  (build hm "/igpop")

  (srv)

  (handler {:uri "/" :request-method :get}))
