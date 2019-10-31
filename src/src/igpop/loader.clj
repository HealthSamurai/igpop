(ns igpop.loader
  (:require
   [clj-yaml.core]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn read-yaml [pth]
  (clj-yaml.core/parse-string
   (slurp pth)))

(defn enrich [ctx pth obj]
  (let [base (-> (get-in ctx (into [:base :profiles] pth)) (dissoc :elements))]
    (if-let [els (:elements obj)]
      (let [els' (reduce (fn [acc [k v]]
                           (let [next-pth (into pth [:elements k])]
                             (if (get-in ctx (into [:base :profiles] next-pth))
                               (assoc acc k (enrich ctx next-pth v))
                               (if-let [tp (:type base)]
                                 (assoc acc k (enrich ctx [(keyword tp) :elements k] v))
                                 acc))))
                         els els)]
        (assoc (merge base obj) :elements els'))
      (merge base obj))))

(defn profile? [nm]
  (and (str/starts-with? nm "pr.")
       (str/ends-with? nm ".yaml")))

(defn profile-name [nm]
  (str/replace nm #"(^pr\.|\.yaml$)" ""))

(defn valueset-name [nm]
  (str/replace nm #"(^vs\.|\.yaml$)" ""))

(profile-name "pr.Patient.yaml")
(profile-name "pr.Patient.yaml")

(defn load-defs [ctx pth]
  (let [manifest (read-yaml (str pth "/ig.yaml"))
        files (.listFiles (io/file (str pth "/src")))]
    (->> files
         (reduce
          (fn [acc f]
            (let [nm (.getName f)]
              (cond
                (.isDirectory f)
                (let [rt (keyword nm)]
                  (reduce (fn [acc f]
                            (let [sub-nm (.getName f)]
                              (if (profile? sub-nm)
                                (let [source (read-yaml (.getPath f))
                                      prof-name (keyword (profile-name sub-nm))]
                                  (-> acc
                                      (assoc-in [:sources rt prof-name] source)
                                      (assoc-in [:profiles rt prof-name] (enrich ctx [rt] source))))
                                acc)))
                          acc (.listFiles f)))

                (str/starts-with? nm "vs.")
                (let [rt (valueset-name nm)]
                  (assoc-in acc [:valuesets (keyword rt)]
                            (read-yaml (.getPath f))))

                (profile? nm)
                (let [rt (keyword (profile-name nm))]
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
          manifest' (assoc manifest :base fhir :home home)]
      (merge
       manifest'
       (load-defs manifest' home)))))

(defn reload [ctx]
  (swap! ctx
         (fn [{home :home :as ctx}]
           (println "??" )
           (merge
            (dissoc ctx :profiles :sources :valuesets)
            (read-yaml (io/file home "ig.yaml"))
            (load-defs ctx home)))))



