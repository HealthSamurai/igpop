(ns igpop.parser
  (:require [clojure.string :as str]))

(defn parse-lines [s]
  (->>
   (str/split-lines s)
   (map-indexed
    (fn [i ln]
      (let [len (count ln)
            ltrim-s (str/replace ln #"^\s*" "")
            ident (- len (count ltrim-s))]
        {:ln i
         ;; decremented on nested block parse
         :ident ident
         ;; immutable position
         :pos ident
         :text ltrim-s})))))

(def key-regex #"(^[-_a-zA-Z0-9]+):")
(re-find key-regex "aaa: 1")

(defn block-type [[{txt :text :as ln} & lns]]
  (when txt
    (cond
      (str/starts-with? txt "-") :coll
      (re-find key-regex  txt) :map
      :else :unknown)))



(defn parse-inline [{ln :ln txt :text pos :pos :as l}]
  {:type :str
   :value (str/trim txt)
   :block {:from {:ln ln :pos pos}
           :to   {:ln ln :pos (+ pos (count txt))}}})

(declare parse-block)

(defn parse-entry [{txt :text ident :ident ln :ln pos :pos :as line}]
  (when-not  (= 0 ident)
    (println "ERROR:" (str "Expected only ident = 0; got " line)))
  (let [start {:ln ln :pos pos}
        end   {:ln ln :pos (+ pos (count txt))}]
    (if (or (str/blank? txt) (not (= 0 ident)))
      {:type :kv
       :kind :newline
       :block {:from start :to end}}
      (if-let [[kk k] (re-find key-regex txt)]
        (let [kk-len (count kk)
              k (keyword k)
              lrest (subs txt kk-len)]
          (if (str/blank? lrest)
            {:type :kv
             :kind :block
             :key k
             :block {:from start :to end}}
            (if (re-matches #"^\s*\|\s*$" lrest)
              {:type :kv
               :kind :text-multiline
               :key k
               :block {:from start}}
              (let [value (parse-inline {:text lrest
                                         :ln ln
                                         :ident 0
                                         :pos (+ pos kk-len)})]
                {:type :kv
                 :kind :inline
                 :key k
                 :block {:from start :to (get-in value [:block :to])}
                 :value value}))))
        {:type :kv
         :kind :key-start
         :key txt
         :block {:from start :to end}})))
  )


(defn collect-block-lines
  "collect lines untill next start and return rest of lines"
  [lns]
  (loop [[ln' & lns' :as all-lns'] lns
         block-lines []]
    (cond
      (nil? ln')
      [block-lines lns']
      (= 0 (:ident ln'))
      [block-lines all-lns']
      :else
      (recur lns' (conj block-lines (update ln' :ident #(max 0 (- % 2))))))))

(defn parse-entries [acc lines opts]
  (loop [acc acc, [ln & lns] lines, entries []]
    (if (nil? ln)
      entries
      (let [{kind :kind :as entry} (parse-entry ln)]
        (cond
          ;; inline brench
          (contains? #{:inline :newline :key-start} kind)
          (recur acc lns (conj entries entry))

          ;; if block element - collect related lines and parse
          (= kind :block)
          (let [[block-lines lns'] (collect-block-lines lns)
                value (when-not (empty? block-lines) (parse-block acc block-lines opts))
                entry' (if value
                         (-> (assoc entry :value value)
                             (assoc-in [:block :to] (get-in value [:block :to])))
                         (assoc entry :kind :inline))]
            (recur acc lns' (conj entries entry')))

          :else
          (do 
            (println "ERROR:" (str "Unknown entry" entry))
            (recur acc lns entries)))))))

(defn parse-map [acc lines opts]
  (let [entries (parse-entries acc lines opts)]
    {:type :map
     :block {:from (get-in (first entries) [:block :from])
             :to (get-in (last entries) [:block :to])}
     :value entries}))

(defn parse-collection [acc lines opts]
  (let [entries (loop [acc acc
                       [{txt :text :as ln} & lns] lines
                       entries []]
                  (if (nil? ln)
                    entries
                    (if (str/blank? txt)
                      {:type :coll-entry
                       :block {:from {:ln (:ln ln) :pos (:pos ln)} 
                               :to {:ln (:ln ln) :pos (:pos ln)}}
                       :value {:type :newline}}
                      (let [[kk k] (re-find #"(^-\s*)" txt)
                            kk-len (count kk)
                            rest-text (subs txt kk-len)
                            first-line {:text rest-text
                                        :ln (:ln ln)
                                        :ident 0
                                        :pos (+ (:pos ln) (dec kk-len))}
                            [entry-lines lns'] (collect-block-lines lns)
                            value (cond
                                    (and (not (empty? entry-lines)))
                                    (parse-block acc
                                                 (if-not (str/blank? rest-text)
                                                   (into [first-line] entry-lines)
                                                   entry-lines)
                                                 opts)

                                    (and (not (str/blank? rest-text)) (empty? entry-lines))
                                    (parse-inline first-line)

                                    :else
                                    {:type :unknown :lines entry-lines :text rest-text})]

                        (recur acc lns' (conj entries
                                             {:type :coll-entry
                                              :block {:from {:ln (:ln ln) :pos (:pos ln)} 
                                                      :to   (get-in value [:block :to])}
                                              :value value}))))))]

    {:type :coll
     :block {:from (get-in (first entries) [:block :from])
             :to (get-in (last entries) [:block :to])}
     :value entries}))


(defn parse-block [acc lines opts]
  (let [type (block-type lines)]
    (if (= :coll type) (parse-collection acc lines opts)
        (parse-map acc lines opts))))

(defn parse [s & [opts]]
  (parse-block {} (parse-lines s) opts))
