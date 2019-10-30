(ns igpop.loader
  (:require
   [clj-yaml.core]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [igpop.site.profiles]
   [igpop.site.valuesets :as valuesets]
   [igpop.site.views :as views]
   [org.httpkit.server]
   [ring.middleware.head]
   [ring.util.codec]
   [ring.util.response]
   [route-map.core]))

(defn read-yaml [pth]
  (clj-yaml.core/parse-string
   (slurp pth)))

(defn load-defs [pth]
  (let [manifest (read-yaml (str pth "/ig.yaml"))
        files (file-seq (io/file (str pth "/src")))]
    (->> files
         (reduce
          (fn [acc f]
            (let [nm (.getName f)]
              (cond
                (str/starts-with? nm "vs.")
                (let [rt (str/replace nm #"\.yaml$" "")]
                  (assoc-in acc [:valuesets (keyword rt)]
                            (read-yaml (.getPath f))))
                (and (str/ends-with? nm ".yaml"))
                (let [rt (str/replace nm #"\.yaml$" "")]
                  (println "Load.." rt)
                  (assoc-in acc [:profiles (keyword rt) :basic]
                            (read-yaml (.getPath f))))
                :else
                (do (println "TODO" nm) acc)

                ))) {}))))

(defn enrich [base profile]
  (if-let [els (:elements profile)]
    (let [els' (reduce (fn [acc [k v]]
                         (if-let [base-element (get-in base [:elements k])]
                           (assoc acc k (enrich base-element v))
                           acc)) els els)]
      (assoc (merge (dissoc base :elements) profile) :elements els'))
    (merge (dissoc base :elements) profile)))

(defn safe-file [& pth]
  (let [file (apply io/file pth)]
    (when (.exists file) file)))

(defn load-fhir [home fhir-version]
  (if-let [fhir-dir (safe-file home "node_modules" (str "igpop-fhir-" fhir-version))]
    (->> (file-seq fhir-dir)
         (reduce (fn [acc f]
                   (println "Loading: " (.getName f))
                   (let [nm (.getName f)]
                     (cond
                       (str/starts-with? nm "vs.")
                       (let [rt (str/replace nm #"\.yaml$" "")]
                         (assoc-in acc [:valuesets (keyword rt)]
                                   (read-yaml (.getPath f))))

                       (and (str/ends-with? nm ".yaml"))
                       (let [rt (str/replace nm #"\.yaml$" "")]
                         (println "Load.." rt)
                         (assoc-in acc [:profiles (keyword rt)] (read-yaml (.getPath f))))
                       ))) {}))

    (println "Could not find " (.getPath (io/file home "node_modules" (str "igpop-fhir-" fhir-version))))
    ))


(defn load-project [home]
  (let [manifest-file (io/file home "ig.yaml")]
    (when-not (.exists manifest-file)
      (throw (Exception. (str "Manifest " (.getPath manifest-file) " does not exists"))))

    (let [manifest (read-yaml (.getPath manifest-file))
          fhir (when-let [fv (:fhir manifest)] (load-fhir home fv))]
      (merge
       (assoc manifest :fhir fhir)
       (load-defs home))

      )))
