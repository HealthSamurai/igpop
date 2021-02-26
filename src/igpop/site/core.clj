(ns igpop.site.core
  (:require
   [clojure.string :as str]
   [igpop.loader]
   [igpop.site.profiles]
   [igpop.site.valuesets]
   [igpop.site.codesystems]
   [igpop.site.structure-definitions :as site-sd]
   [igpop.site.docs]
   [igpop.site.packages]
   [igpop.site.views :as views]
   [igpop.structure-definition :as sd]
   [igpop.fhir-package :as fp]
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
  (let [ sd-id (-> req
                        (get :uri)
                        (clojure.string/replace #"/get-profile/" "")
                        (clojure.string/split #"&"))
        file-name-yaml (if (= "basic" (last  sd-id))
                         (str (first  sd-id) ".yaml")
                         (str (clojure.string/join "/"  sd-id) ".yaml"))
        file-name-igpop (if (= "basic" (last  sd-id))
                          (str (first  sd-id) ".igpop")
                          (str (clojure.string/join "/"  sd-id) ".igpop"))]
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
  (let [ sd-id (-> req
                        (get :uri)
                        (clojure.string/replace #"/post-profile/" "")
                        (clojure.string/split #"&"))
        file-name-yaml (if (= "basic" (last  sd-id))
                         (str (first  sd-id) ".yaml")
                         (str (clojure.string/join "/"  sd-id) ".yaml"))
        file-name-igpop (if (= "basic" (last  sd-id))
                          (str (first  sd-id) ".igpop")
                          (str (clojure.string/join "/"  sd-id) ".igpop"))]
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

(defn get-resource-sd [ctx req]
  (let [raw-sd-id (get-in req [:route-params :sd-id])
        html? (or (str/ends-with? raw-sd-id ".html")
                  (not (str/ends-with? raw-sd-id ".json")))
        sd-id (str/replace raw-sd-id #".(json|html)$" "")]
    (if-let [resource (get-in ctx [:structure-definitions sd-id])]
      (if html?
        (site-sd/structure-definition ctx req)
        {:status 200 :body (cheshire.core/generate-string resource {:pretty true})})
      #_{:status 200
       :body (if html?
               (site-sd/structure-definition ctx req)
               #_(views/layout ctx
                             [:pre (cheshire.core/generate-string resource {:pretty true})]
                             [:div.content
                              "Resource Profile: " (:title resource)
                              #_[:pre json-resource]])
               (cheshire.core/generate-string resource {:pretty true}))}
      {:status 404 :body "File not found!"})
    #_(if (get-in ctx [:path-by-sd-id sd-id])
        (let [path     (get-in ctx [:path-by-sd-id sd-id])
              rt       (first path)
              id       (last path)
              profile  (get-in ctx (into [:diff-profiles] path))
              snapshot (get-in ctx (into [:snapshot] path))
              resource (if (or (sd/path-extension-root? path) (sd/path-extension? path))
                         (sd/extension->structure-definition ctx rt (sd/path->sd-path path) profile snapshot)
                         (sd/profile->structure-definition ctx rt id profile snapshot))
              json-resource (cheshire.core/generate-string resource {:pretty true})]
          {:status 200
           :body (if html?
                   (views/layout ctx
                                 [:pre json-resource]
                                 [:div.content
                                  "Resource Profile: " (:title resource)
                                  #_[:pre json-resource]])
                   json-resource)})
        {:status 404 :body "File not found!"})))

(defn get-valueset-sd [ctx req]
  (let [vs-id (str/replace (get-in req [:route-params :vs-id]) #".json$" "")
        valueset (get-in ctx [:structure-definitions vs-id])]
    (if valueset
      {:status 200 :body (cheshire.core/generate-string valueset {:pretty true})}
      {:status 404 :body "File not found!"})))


#_(defn get-valueset-sd [ctx req]
  (let [vs-id (str/replace (get-in req [:route-params :vs-id]) #".json$" "")
        path (get-in ctx [:path-by-vs-id vs-id])]
    (if path
      {:status 200
       :body (cheshire.core/generate-string
              (sd/ig-vs->valueset ctx [(last path) (get-in ctx (into [:valuesets] path))])
              {:pretty true})}
      {:status 404 :body "File not found!"})))


(defn get-codesystem-sd [ctx req]
  (let [cs-id (str/replace (get-in req [:route-params :cs-id]) #".json$" "")
        resource (get-in ctx [:structure-definitions cs-id])]
    (if resource
      {:status 200 :body (cheshire.core/generate-string resource {:pretty true})}
      {:status 404 :body "File not found!"})))

#_(defn get-codesystem-sd [ctx req]
  (let [cs-id (str/replace (get-in req [:route-params :cs-id]) #".json$" "")
        path (get-in ctx [:path-by-cs-id cs-id])]
    (if path
      {:status 200
       :body (cheshire.core/generate-string
              (sd/ig-cs->codesystem ctx [(last path) (get-in ctx (into [:codesystems] path))])
              {:pretty true})}
      {:status 404 :body "File not found!"})))


(def routes
  {:GET #'welcome
   "ig.yaml" {:GET #'source}
   "lsp" {:GET #'lsp}
   "docs" {:GET #'igpop.site.docs/dashboard
           [:doc-id] {[:curr-doc] {:GET #'igpop.site.docs/doc-page}}}
   "valuesets" {:GET #'igpop.site.valuesets/valuesets-dashboard
                [:valuset-id] {:GET #'igpop.site.valuesets/valueset}}
   "codesystems" {:GET #'igpop.site.codesystems/codesystems-dashboard
                  [:codesystem-id] {:GET #'igpop.site.codesystems/codesystem}}
   "get-profile" {[:profile-id] {:GET #'get-profile}}
   "post-profile" {[:profile-id] {:POST #'post-profile!}}
   "edit" {[:profile-id] {:GET #'edit}}
   "profiles" {:GET #'igpop.site.profiles/profiles-dashboard
               [:resource-type] {:GET #'igpop.site.profiles/profile
                                 [:profile] {:GET #'igpop.site.profiles/profile}}}
   "StructureDefinition" {:GET #'igpop.site.structure-definitions/structure-definitions-dashboard
                          [:sd-id] {:GET #'get-resource-sd}}
   "ValueSet" {[:vs-id] {:GET #'get-valueset-sd}}
   "CodeSystem" {[:cs-id] {:GET #'get-codesystem-sd}}})

(defn dynamic-routes [ctx]
  (let [m (sd/npm-manifest ctx)]
    {(str (:name m) ".zip") {:GET #'igpop.site.packages/npm-package}
     "package.tgz" {:GET #'igpop.site.packages/fhir-package}})) ;; Eventually 'package.tgz' became static link. TODO: maybe move to another place

(defn *dispatch [ctx {uri :uri meth :request-method :as req}]
  (let [uri (str/replace uri #"\.html$" "")
        req (assoc req :uri uri)
        r (merge (deref #'routes) (dynamic-routes ctx))]
    (if-let [{handler :match params :params} (route-map.core/match [meth uri] r)]
      (handler ctx (assoc req :route-params params))
      {:status 404 :body "Not Found"})))

(defn dispatch [ctx {uri :uri meth :request-method :as req}]
  (or
   (handle-static req)
   (do
     (igpop.loader/reload ctx)
     (swap! ctx sd/ctx-build-sd-indexes)
     (swap! ctx sd/ctx-build-structure-definitions) ;; REVIEW: is that good thing to preserve all structure definitions IN_MEMORY ?
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
                (assoc-in [:flags :no-edit] true)
                (sd/ctx-build-sd-indexes)
                (sd/ctx-build-structure-definitions) ;; REVIEW: is that good thing to preserve all structure definitions IN_MEMORY ?
                )
        build-dir (io/file home "build")]
    (.mkdir build-dir)

    (.mkdir (io/file build-dir "profiles"))
    (dump-page ctx home [] :index)
    (dump-page ctx home ["profiles"] :index)
    (doseq [[rt prs] (:profiles ctx)]
      (doseq [[id pr] (if-not (some #(= % :basic) (keys prs))
                        (assoc prs :basic {})
                        prs)]
        (dump-page ctx home ["profiles" (name rt) (name id) {:format "html"}])))

    (.mkdir (io/file build-dir "valuesets"))
    (dump-page ctx home ["valuesets"] :index)
    (doseq [[id _] (get-in ctx [:valuesets])]
      (dump-page ctx home ["valuesets" (name id) {:format "html"}]))

    (.mkdir (io/file build-dir "codesystems"))
    (dump-page ctx home ["codesystems"] :index)
    (doseq [[id _] (get-in ctx [:codesystems])]
      (dump-page ctx home ["codesystems" (name id) {:format "html"}]))

    (.mkdir (io/file build-dir "StructureDefinition"))
    (doseq [sd-id (keys (:path-by-sd-id ctx))]
      (dump-page ctx home ["StructureDefinition" sd-id {:format "html"}])
      (dump-page ctx home ["StructureDefinition" sd-id {:format "json"}]))

    (.mkdir (io/file build-dir "ValueSet"))
    (doseq [vs-id (keys (:path-by-vs-id ctx))]
      (dump-page ctx home ["ValueSet" vs-id])
      (dump-page ctx home ["ValueSet" vs-id {:format "json"}]))

    (.mkdir (io/file build-dir "CodeSystem"))
    (doseq [cs-id (keys (:path-by-cs-id ctx))]
      (dump-page ctx home ["CodeSystem" cs-id])
      (dump-page ctx home ["CodeSystem" cs-id {:format "json"}]))

    (.mkdir (io/file build-dir "docs"))
    (dump-page ctx home ["docs"] :index)
    (doseq [[doc-id doc] (get-in ctx [:docs :pages])]
      (doseq [[cur _] (if (:basic doc) doc (assoc doc :basic {}))]
        (dump-page ctx home ["docs" (name doc-id) (name cur) {:format "html"}])))

    (.mkdir (io/file build-dir "static"))
    (doseq [f (str/split (get-static) #" ")]
      (when-not (or (= f "static-resources") (= f "static-resources\n"))
        (io/copy (io/input-stream (io/resource (str "public/" f)))
                 (io/file build-dir "static" (last (str/split f #"/"))))))
    (sd/generate-package! :npm ctx)
    (fp/generate-fhir-package! ctx)))

(comment

  (def hm (.getAbsolutePath (io/file  "example")))
  (def hm (.getAbsolutePath (io/file  "../ig-ae")))

  (def srv (start hm 8899))

  (build hm "http://localhost/igpop/example/build")
  (build hm "/igpop")

  (srv) ;; stop

  ;; (apply u/href {} ["StructureDefinition" (sd/make-profile-id (:id sd/ctx) :Adress :basic) {:format "json"}])

  ;; (handler {:uri "/" :request-method :get})
  )
