(ns igpop.parser
  (:require
   [clojure.java.io :as io]
   [edamame.core]
   [zen.core :as zen]
   [clojure.string :as str])
  (:import [java.io StringReader]))


(defn str-reader [s]
  (io/reader (StringReader. s)))


(defn parse-value [k lns]
  (let [v (->> lns (filterv identity) (str/join "\n"))]
    (if (str/ends-with? (name k) ">")
      v
      (try
        (edamame.core/parse-string v)
        (catch Exception e
          (println :parse-error v)
          (str "Error: " e))))))

(defn add-link [ztx to from path]
  (swap! ztx assoc-in [:zd/links to path from] {}))

(defn collect-back-links [ztx nm path doc]
  (cond

    (map? doc)
    (doseq [[k v] doc]
      (collect-back-links ztx nm (conj path k) v))
    (sequential? doc)
    (->> doc
         (map-indexed (fn [k v]
                        (collect-back-links ztx nm (conj path k) v)))
         (doall))

    (set? doc)
    (doseq [v doc]
      (collect-back-links ztx nm path v))

    (symbol? doc)
    (add-link ztx doc nm path)))

(defn back-refs [ztx nm doc]
  (collect-back-links ztx nm [] (dissoc doc :zd/name)))

(defn parse [ztx md]
  (loop [res {:-keys []}
         [l & ls] (line-seq (str-reader md))
         state :start
         current-key nil
         current-acc nil]
    (if (and (nil? l) (empty? ls))
      (if current-key
        (-> (assoc res current-key (parse-value current-key current-acc))
            (update :-keys conj current-key))
        res)
      (if (str/starts-with? l ":")
        (let [res         (if current-key (-> (assoc res current-key (parse-value current-key current-acc))
                                              (update :-keys conj current-key)) res)
              [a b]       (str/split l #"\s+" 2)
              current-key (keyword (subs a 1))
              current-acc [b]]
          (recur res ls :in-key current-key current-acc))
        (if (= :in-key state)
          (recur res ls state current-key (conj current-acc l))
          (recur (if (not (str/blank? l)) (update res :?> conj l) res) ls state current-key current-acc))))))

(defn name-to-path [ztx nm]
  (str (:zd/path @ztx) "/" (str/replace nm #"\." "/") ".zd"))

(defn path-to-name [ztx p]
  (let [pth (get @ztx :zd/path)]
    (-> (subs p (inc (count pth)))
        (str/replace #"\.zd$" "")
        (str/replace #"/" ".")
        (symbol))))

(defn load-doc [ztx nm cnt & [props]]
  (let [doc (-> (parse ztx cnt)
                (assoc :zd/name nm)
                (merge props))]
    (swap! ztx assoc-in [:zd/resources nm] doc)
    (back-refs ztx nm doc)))

(defn load-file [ztx f]
  (let [p (.getPath f)]
    (when (and (str/ends-with? p ".zd")
               (not (str/starts-with? (.getName f) ".")))
      (when (.exists f)
        (load-doc ztx (path-to-name ztx p) (slurp f))))))

(defn read-doc [ztx nm]
  (let [pth (name-to-path ztx nm)
        f   (io/file pth)]
    (when(.exists f)
      (let [cnt (slurp f)]
        (load-doc ztx nm cnt)))))

(defn get-doc [ztx nm]
  (get-in @ztx [:zd/resources nm]))

(defn get-links [ztx nm]
  (get-in @ztx [:zd/links nm]))

(comment

  (def ztx (zen/new-context {:zd/path "zd"}))

  (read-doc ztx 'zd.features.format)




  )
