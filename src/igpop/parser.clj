(ns igpop.parser
  (:require [clojure.string :as str]))

;; TODO:
;; inline + obj + coll
;; comments
;; multiline txt
;; collect value
;; collect parse errors

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

(def inline-type-regex
  [[:map #"^\s*\{"]
   [:coll #"^\s*\["]
   [:str-q #"^\s*'"]
   [:str-dq #"^\s*\""]
   [:true #"^\s*true"]
   [:false #"^\s*false"]
   [:null #"^\s*null"]
   [:num #"\s*\d+\.\d*"]
   [:int #"\s*\d+"]])

(defmulti read-inline
  (fn [tp txt ln pos] tp))

(defmethod read-inline
  :default
  [tp txt ln pos]
  (println "ERROR: don't know how to read " tp " - " txt))

(defn inline-type [txt]
  (or 
   (loop [[[tp regex] & ms] inline-type-regex]
     (when-not (nil? tp)
       (if (re-find regex txt)
         tp
         (recur ms))))
   :alphanum))

(defmethod read-inline :int
  [_ txt ln pos]
  (let [[_ spaces int rest] (re-find #"(\s*)(\d+)(.*)" txt)]
    [{:type :int
      :block {:from {:pos pos :ln ln} :to {:pos (+ pos (count int)) :ln ln}}
      :value (Integer/parseInt int)}
     rest]))

(defmethod read-inline :str-dq
  [_ txt ln pos]
  (let [s (subs (str/trim txt) 1)]
    (if-let [idx (str/index-of s "\"")]
      (let [res (subs s 0 idx)]
        [{:type :str
          :block {:from {:pos pos :ln ln} :to {:pos (+ pos (inc idx)) :ln ln}}
          :value res}
         (subs txt (+ idx 2))])

      [{:type :str
        :block {:from {:pos pos :ln ln} :to {:pos (+ pos (count txt)) :ln ln}}
        :value txt
        :error "Unterminated string. Missed \"!"}

       nil])))

(defmethod read-inline :str-q
  [_ txt ln pos]
  (let [s (subs (str/trim txt) 1)]
    (if-let [idx (str/index-of s "'")]
      (let [res (subs s 0 idx)]
        [{:type :str
          :block {:from {:pos pos :ln ln} :to {:pos (+ pos (inc idx)) :ln ln}}
          :value res}
         (subs txt (+ idx 2))])

      [{:type :str
        :block {:from {:pos pos :ln ln} :to {:pos (+ pos (count txt)) :ln ln}}
        :value txt
        :error "Unterminated string. Missed '!"}

       nil])))

(defmethod read-inline :true
  [_ txt ln pos]
  (let [idx (str/index-of txt "true")]
    (let [res (subs txt 0 idx)]
      [{:type :bool
        :block {:from {:pos pos :ln ln} :to {:pos (+ pos (+ idx 3)) :ln ln}}
        :value true}
       (subs txt (+ idx 4))])))

(defmethod read-inline :false
  [_ txt ln pos]
  (let [idx (str/index-of txt "false")]
    (let [res (subs txt 0 idx)]
      [{:type :bool
        :block {:from {:pos pos :ln ln} :to {:pos (+ pos (+ idx 4)) :ln ln}}
        :value false}
       (subs txt (+ idx 5))])))


(defmethod read-inline :key
  [_ txt ln pos]
  (if-let [[_ k rest] (re-find #"^\s*([-_a-zA-Z0-9]+):(.*)" txt)]
    [{:type :key
      :block {:from {:pos pos :ln ln} :to {:pos (+ pos (inc (count k))) :ln ln}}
      :value (keyword k)}
     rest]
    nil))

(defmethod read-inline :alphanum
  [_ txt ln pos]
  (if-let [[_ k rest] (re-find #"^\s*([-_a-zA-Z0-9]+)(.*)" txt)]
    [{:type :str
      :block {:from {:pos pos :ln ln} :to {:pos (+ pos (inc (count k))) :ln ln}}
      :value k}
     rest]
    nil))

(declare do-read)

(defmethod read-inline :map
  [_ txt ln pos]
  (let [block {:from {:pos pos :ln ln} :to {:pos (+ pos (count txt)) :ln ln}}]
    (if (re-matches #"^\s*\{\s*\}\s*$" txt)
      [{:type :map
        :kind :empty
        :block block} nil]
      (let [[_ kvs] (re-find #"\s*\{(.*)" txt)
            entries (loop [rest kvs, pos pos, entries [] i 0]
                      (if (or (nil? rest) (str/blank? rest))
                        entries
                        (if-let [[{k :value} rest'] (read-inline :key rest ln pos)]
                          (let [pos-diff (- (count rest) (count rest'))
                                [v rest''] (do-read rest' ln (+ pos pos-diff))
                                entry {:type :kv
                                       :kind :inline
                                       :key k
                                       :block (assoc-in block [:to :pos] (+ pos pos-diff))
                                       :value v}
                                entries (conj entries entry)]
                            (if (nil? rest'')
                              entries
                              (if-let [[_ rest'''] (re-matches #"\s*,(.*)" rest'')]
                                (if-not (str/blank? (str/trim rest'''))
                                  (recur rest''' (+ pos (- (count rest) (count rest'''))) entries (inc i))
                                  entries)
                                (if (re-matches #"\s*}(.*)" rest'')
                                  entries
                                  (assert false rest''))))))))]
        [{:type :map
          :kind :inline
          :block block
          :value entries}]))))

(defn do-read [txt ln pos]
  (let [tp (inline-type txt)]
    (read-inline tp txt ln pos)))

(defn parse-inline [{ln :ln txt :text pos :pos :as l}]
  (let [[v rest] (do-read txt ln pos)]
    v))

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
         :error "Expected key closed by ':'"
         :key txt
         :block {:from start :to end}}))))


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
  (loop [acc acc, [ln & lns] lines, entries [], ks #{}]
    (if (nil? ln)
      entries
      (let [{k :key kind :kind :as entry} (parse-entry ln)
            entry (if (and k (contains? ks k))
                    (assoc entry :error (str "Duplicate key " (name k)))
                    entry)
            ks (if (:key entry) (conj ks (:key entry)) ks)]
        (cond
          ;; inline brench
          (contains? #{:inline :newline :key-start} kind)
          (recur acc lns (conj entries entry) ks)

          ;; if block element - collect related lines and parse
          (= kind :block)
          (let [[block-lines lns'] (collect-block-lines lns)
                value (when-not (empty? block-lines) (parse-block acc block-lines opts))
                entry' (if value
                         (-> (assoc entry :value value)
                             (assoc-in [:block :to] (get-in value [:block :to])))
                         (assoc entry :kind :inline))]
            (recur acc lns' (conj entries entry') ks))

          (= kind :text-multiline)
          (let [[block-lines lns'] (collect-block-lines lns)
                txt (->> block-lines (mapv :text) (str/join "\n"))
                from (select-keys (or (first block-lines) ln) [:ln :pos])
                to (select-keys (or (last block-lines) ln) [:ln :pos])
                entry' (-> (assoc entry :value {:type :str
                                                :block {:from from :to to}
                                                :value txt})
                           (assoc-in [:block :to] to))]
            (recur acc lns' (conj entries entry') ks))

          :else
          (do 
            (println "ERROR:" (str "Unknown entry" entry))
            (recur acc lns entries ks)))))))

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
                      (recur acc lns (conj entries
                                           {:type :coll-entry
                                            :block {:from {:ln (:ln ln) :pos (:pos ln)} 
                                                    :to {:ln (:ln ln) :pos (:pos ln)}}
                                            :value {:type :newline}}))
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


(defn collect-errors [errs {val :value tp :type err :error block :block :as ast}]
  (let [errs (if err (conj errs {:message err :block block}) errs)
        val (when val (if (and (sequential? val) (not (string? val)))
                        val [val]))]
    (if val
      (reduce collect-errors errs val)
      errs)))

(defn errors [ast]
  (collect-errors [] ast))
