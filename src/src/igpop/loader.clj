(ns igpop.loader
  (:require
   [clj-yaml.core]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn read-yaml [pth]
  (clj-yaml.core/parse-string
   (slurp pth)))

(defn enrich [ctx pth obj]
  (let [base (-> (get-in ctx (into [:fhir :profiles] pth)) (dissoc :elements))]
    (if-let [els (:elements obj)]
      (let [els' (reduce (fn [acc [k v]]
                           (let [next-pth (into pth [:elements k])]
                             (if (get-in ctx (into [:fhir :profiles] next-pth))
                               (assoc acc k (enrich ctx next-pth v))
                               (if-let [tp (:type base)]
                                 (assoc acc k (enrich ctx [(keyword tp) :elements k] v))
                                 acc))))
                         els els)]
        (assoc (merge base obj) :elements els'))
      (merge base obj))))

(defn load-defs [ctx pth]
  (let [manifest (read-yaml (str pth "/ig.yaml"))
        files (.listFiles (io/file (str pth "/src")))]
    (->> files
         (reduce
          (fn [acc f]
            (println "f:" (.getPath f))
            (let [nm (.getName f)]
              (cond
                (.isDirectory f)
                (let [rt (keyword (str/replace nm #"\.yaml$" ""))]
                  
                  (reduce (fn [acc f]
                            (println "Sub file" f)
                            (let [sub-nm (.getName f)]
                              (if (and (not (str/starts-with? sub-nm "vs."))
                                       (str/ends-with? sub-nm ".yaml"))
                                (let [source (read-yaml (.getPath f))
                                      prof-name (keyword (str/replace sub-nm #"\.yaml$" ""))]
                                  (-> acc
                                      (assoc-in [:sources rt prof-name] source)
                                      (assoc-in [:profiles rt prof-name] (enrich ctx [rt] source))))
                                acc)))
                          acc (.listFiles f)))

                (str/starts-with? nm "vs.")
                (let [rt (str/replace nm #"\.yaml$" "")]
                  (assoc-in acc [:valuesets (keyword rt)]
                            (read-yaml (.getPath f))))
                (and (str/ends-with? nm ".yaml"))
                (let [rt (keyword (str/replace nm #"\.yaml$" ""))]
                  (println "Load.." rt)
                  (let [source (read-yaml (.getPath f))]
                    (-> acc
                        (assoc-in [:sources rt :basic] source)
                        (assoc-in [:profiles rt :basic] (enrich ctx [rt] source)))))
                :else
                (do (println "TODO" nm) acc)

                ))) {}))))



(defn safe-file [& pth]
  (let [file (apply io/file pth)]
    (when (.exists file) file)))

(defn load-fhir [home fhir-version]
  (if-let [fhir-dir (safe-file home "node_modules" (str "igpop-fhir-" fhir-version))]
    (->> (file-seq fhir-dir)
         (reduce (fn [acc f]
                   (let [nm (.getName f)]
                     (cond
                       (str/starts-with? nm "vs.")
                       (let [rt (str/replace nm #"\.yaml$" "")]
                         (assoc-in acc [:valuesets (keyword rt)]
                                   (read-yaml (.getPath f))))

                       (and (str/ends-with? nm ".yaml"))
                       (let [rt (str/replace nm #"\.yaml$" "")]
                         (assoc-in acc [:profiles (keyword rt)] (read-yaml (.getPath f))))
                       ))) {}))
    (println "Could not find " (.getPath (io/file home "node_modules" (str "igpop-fhir-" fhir-version))))))



(defn load-project [home]
  (let [manifest-file (io/file home "ig.yaml")]
    (when-not (.exists manifest-file)
      (throw (Exception. (str "Manifest " (.getPath manifest-file) " does not exists"))))

    (let [manifest (read-yaml (.getPath manifest-file))
          fhir (when-let [fv (:fhir manifest)] (load-fhir home fv))
          manifest' (assoc manifest :fhir fhir :home home)]
      (merge
       manifest'
       (load-defs manifest' home)))))

(defn reload [ctx]
  (reset! ctx
          (merge (dissoc @ctx :profiles :sources :valuesets)
                 (load-defs @ctx (:home @ctx)))))



