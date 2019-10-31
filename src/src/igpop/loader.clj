(ns igpop.loader
  (:require
   [clj-yaml.core]
   [clojure.java.io :as io]
   [clojure.data.csv :as csv]
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

(def prefixes
  {
   :doc {:to [:docs]}
   :pr  {:to [:source]}
   :vs  {:to [:valuesets]}
   })

(def formats
  {:yaml :yaml
   :csv  :csv})

(defn parse-name
  ([dir file-name]
   (let [parts (mapv keyword (str/split file-name #"\."))]
     (if (<= 3 (count parts))
       (let [tp (first parts)
             fmt' (last parts)
             pth (into [] (rest (butlast parts)))
             node (get prefixes tp)
             fmt (get formats fmt')]
         (if (and fmt node)
           {:to (into (:to node)
                      (if (= :pr tp)
                        (into [(keyword dir)] pth)
                        pth))
            :format fmt}
           (println "Could not parse " file-name))))))
  ([file-name]
   (let [parts (mapv keyword (str/split file-name #"\."))]
     (if (<= 3 (count parts))
       (let [tp (first parts)
             fmt' (last parts)
             pth (into [] (rest (butlast parts)))
             node (get prefixes tp)
             fmt (get formats fmt')]

         (if (and fmt node)
           {:to (into (:to node)
                      (if (= :pr tp)
                        (into [(first pth) :basic] (rest pth))
                        pth))
            :format fmt}
           (println "Could not parse " file-name)))))))

(defmulti read-file (fn [fmt _] fmt))
(defmethod read-file :yaml
  [_ pth]
  (read-yaml pth))

(defmethod read-file :csv
  [_ pth]
  (let [[headers & rows] (csv/read-csv (io/reader pth))
        ks (->> headers
                (mapv (fn [k] (keyword (str/trim k)))))]
    (->> rows
         (mapv (fn [rows]
                 (zipmap ks (mapv str/trim rows)))))))

(defn load-defs [ctx pth]
  (let [manifest (read-yaml (str pth "/ig.yaml"))
        files (.listFiles (io/file (str pth "/src")))
        user-data (->> files
                       (sort-by #(count (.getName %)))
                       (reduce
                        (fn [acc f]
                          (let [nm (.getName f)]
                            (if (.isDirectory f)
                              (let [rt (keyword nm)]
                                (reduce (fn [acc f]
                                          (if-let [insert (parse-name nm (.getName f))]
                                            (let [source (read-file (:format insert) (.getPath f))]
                                              (assoc-in acc (:to insert) source))
                                            acc))
                                        acc (.listFiles f)))
                              (if-let [insert (parse-name nm)]
                                (let [source (read-file (:format insert) (.getPath f))]
                                  ;; (println "..." insert)
                                  (assoc-in acc (:to insert) source))
                                (do (println "TODO:" nm)
                                    acc))))) {}))
        profiles (reduce
                  (fn [acc [rt profiles]]
                    (reduce (fn [acc [id profile]]
                              (assoc-in acc [rt id]
                                        (enrich ctx [rt] profile))
                              ) acc profiles)
                    ) {} (:source user-data))]
    (assoc user-data :profiles profiles)
    ))


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
           (merge
            (dissoc ctx :profiles :sources :valuesets)
            (read-yaml (io/file home "ig.yaml"))
            (load-defs ctx home)))))



