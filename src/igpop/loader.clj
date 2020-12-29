(ns igpop.loader
  (:require
   [clj-yaml.core]
   [clojure.java.io :as io]
   [clojure.data.csv :as csv]
   [clojure.string :as str]
   [igpop.site.utils :as u]))

(defn read-yaml [pth]
  (clj-yaml.core/parse-string (slurp pth)))

(defn get-inlined-valuesets [{:keys [profiles valuesets] :as ctx}]
  (->> (vals profiles)
       (mapcat vals)
       (mapcat :elements)
       (map val)
       (map :valueset)
       (filter :concepts)
       (map (juxt (comp keyword :id) #(dissoc % :id)))
       (into valuesets)
       (assoc ctx :valuesets)))

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

(defn capitalized? [s]
  (when (and (string? s) (not (str/blank? s)))
    (Character/isUpperCase (first s))))

(defn parse-name
  "Determines by file-name - file format and path for placing file-data in context."
  ([dir file-name]
   (let [parts (str/split file-name #"\.")]
     (cond
       (and (= 2 (count parts))
            (capitalized? dir)
            (= "yaml" (second parts)))

       {:to [:source (keyword dir) (keyword (first parts))]
        :format :yaml}

       (and
        (= 3 (count parts))
        (= "vs" (first parts))
        (= "yaml" (last parts)))

       {:to [:valuesets (keyword (second parts))]
        :format :yaml}

       (and
        (= 3 (count parts))
        (= "vs" (first parts))
        (= "csv" (last parts)))

       {:to [:valuesets (keyword (second parts)) :concepts]
        :format :csv}

       :else nil)))

  ([file-name]
   (let [parts (str/split file-name #"\.")]
     (cond
       (and (= 2 (count parts))
            (capitalized? (first parts))
            (= "yaml" (second parts)))

       {:to [:source (keyword (first parts)) :basic]
        :format :yaml}


       (and (= 3 (count parts))
            (= "vs" (first parts))
            (= "yaml" (nth parts 2)))

       {:to [:valuesets (keyword (second parts))]
        :format :yaml}

       (and (= 3 (count parts))
            (= "vs" (first parts))
            (= "csv" (nth parts 2)))

       {:to [:valuesets (keyword (second parts)) :concepts]
        :format :csv}


       :else
       nil))))

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

(defn read-md-meta [content]
  (if (str/starts-with? content "---")
    (let [lines (str/split-lines content)]
      (loop [[l & ls] (rest lines)
             meta-lines []]
        (cond
          (nil? l)
          [{:error "Expected second --- to close metadata"} content]
          (str/starts-with? l "---")
          [(clj-yaml.core/parse-string (str/join "\n" meta-lines)) (str/join "\n" ls)]
          :else
          (recur ls (conj meta-lines l)))))
    [{} content]))

(defmethod read-file :md
  [_ pth]
  (let [content (slurp pth)
        [meta content] (read-md-meta content)]
    (assoc meta :source content)))

(defn merge-in [m pth v]
  (update-in m pth (fn [x] (if x (merge x v) v))))

(defn build-profiles [ctx mode]
  (->> ctx
       :source
       (reduce (fn [acc [rt profiles]]
                 (reduce (fn [acc [id profile]]
                           (let [rich-profile (enrich ctx [rt] profile)
                                 resources (get-in ctx (into [:base :profiles] [rt]))
                                 snapshot (u/deep-merge resources rich-profile)]
                             (assoc-in acc [rt id]
                                       (cond
                                         (= mode "profiles") rich-profile
                                         (= mode "resources") resources
                                         (= mode "diff-profiles") profile
                                         (= mode "snapshot") snapshot))))
                         acc profiles))
               {})
       (assoc ctx (keyword mode))
       (get-inlined-valuesets)))

(defn load-defs [ctx pth]
  (let [files (.listFiles (io/file (str pth "/src")))
        homepage (first (filter #(= (.getName %) "homepage.md") files))
        user-data (->> files
                       (sort-by #(count (.getName %)))
                       (reduce
                        (fn [acc f]
                          (let [nm (.getName f)]
                            (if (.isDirectory f)
                              (if (= nm "docs")
                                (let [fset (into #{} (.listFiles f))
                                      files (filter some? (conj fset homepage))]
                                  (reduce (fn [acc f]
                                            (if (.isDirectory f)
                                              (reduce (fn [acc file]
                                                        (let [parts (str/split (.getName file) #"\.")
                                                              id (keyword (first parts))
                                                              file-path (.getPath file)]
                                                          (assoc-in acc [:docs :pages (keyword (.getName f)) id] (read-file :md file-path))))
                                                      acc (.listFiles f))
                                              (let [parts (str/split (.getName f) #"\.")
                                                    id (keyword (first parts))
                                                    file-path (.getPath f)]
                                                (cond
                                                  (= "md" (last parts))
                                                  (if (= (first parts) "homepage") ;;Separate welcome page
                                                    (assoc-in acc [:docs :home id] (read-file :md file-path))
                                                    (assoc-in acc [:docs :pages id :basic] (read-file :md file-path)))
                                                  (= "yaml" (last parts))
                                                  (let [res (read-file :yaml file-path)]
                                                    (update-in acc [:docs id] (fn [x] (if x (merge x res) res))))
                                                  :else
                                                  acc))))
                                          acc files))
                                (let [rt (keyword nm)]
                                  (reduce (fn [acc f]
                                            (if-let [insert (parse-name nm (.getName f))]
                                              (let [source (read-file (:format insert) (.getPath f))]
                                                (merge-in acc (:to insert) source))
                                              acc))
                                          acc (.listFiles f))))
                              (if-let [insert (parse-name nm)]
                                (let [source (read-file (:format insert) (.getPath f))]
                                  ;; (println "..." insert)
                                  (merge-in acc (:to insert) source))
                                (do (println "TODO:" f)
                                    acc))))) {}))]
    (-> (merge ctx user-data)
        (build-profiles "resources")
        (build-profiles "profiles")
        (build-profiles "diff-profiles")
        (build-profiles "snapshot"))))

(defn safe-file [& pth]
  (let [file (apply io/file pth)]
    (when (.exists file) file)))

(defn load-fhir [home fhir-version]
  (let [fhir-dir (if-let [dir (safe-file home (str "igpop-fhir-" fhir-version) "src")]
                   dir
                   (safe-file home (str "node_modules/igpop-fhir-" fhir-version) "src"))]
    (if fhir-dir
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
                           (assoc-in acc [:profiles (keyword rt)] (read-yaml (.getPath f))))))) {}))
      (println "Could not find " (.getPath (io/file home (str "igpop-fhir-" fhir-version)))))))

(defn load-definitions [home fhir-version]
  (let [fhir-types (if-let [dir (safe-file home (str "igpop-fhir-" fhir-version) "fhir-types-definition.yaml")]
                     dir
                     (safe-file home (str "node_modules/igpop-fhir-" fhir-version) "fhir-types-definition.yaml"))]
    (if fhir-types
      (read-yaml fhir-types)
      (println "Could not find " (.getPath (io/file home (str "igpop-fhir-" fhir-version) "fhir-types-definition.yaml"))))))

(defn load-and-parse [file-name]
  (let [defaults (safe-file file-name)]
    (read-yaml defaults)))

(defn load-project [home-dir]
  (let [manifest-file (io/file home-dir "ig.yaml")]
    (when-not (.exists manifest-file)
      (throw (Exception. (str "Manifest " (.getPath manifest-file) " does not exists"))))

    (let [manifest (read-yaml (.getPath manifest-file))
          fhir (when-let [fv (:fhir manifest)] (load-fhir home-dir fv))
          definitions (when-let [fv (:fhir manifest)] (load-definitions home-dir fv)) ;; REVIEW:   json-schema loading.
          manifest' (assoc manifest :base fhir :home home-dir :definitions definitions)]
      (merge
       manifest'
       (load-defs manifest' home-dir)))))

(defn reload [ctx]
  (swap! ctx
         (fn [{home :home :as ctx}]
           (merge
            (dissoc ctx :profiles :sources :valuesets)
            (read-yaml (io/file home "ig.yaml"))
            (load-defs ctx home)))))



