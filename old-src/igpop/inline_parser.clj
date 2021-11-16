(ns igpop.inline-parser
  (:require [clojure.string :as str]))


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
            {entries :entries error :error}
            (loop [rest kvs, pos pos, entries [] i 0]
              (if (or (nil? rest) (str/blank? rest))
                {:entries  entries}
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
                      {:entries entries}
                      (if-let [[_ rest'''] (re-matches #"\s*,(.*)" rest'')]
                        (if-not (str/blank? (str/trim rest'''))
                          (recur rest''' (+ pos (- (count rest) (count rest'''))) entries (inc i))
                          {:entries entries})
                        (if (re-matches #"\s*}(.*)" rest'')
                          {:entries entries}
                          {:entries entries :error "Expected } to close map"})))))))]
        [(cond->
             {:type :map
                 :kind :inline
                 :block block
              :value entries}
           error (assoc :error error))]))))

(defn do-read [txt ln pos]
  (let [tp (inline-type txt)]
    (read-inline tp txt ln pos)))

(defn parse-inline [{ln :ln txt :text pos :pos :as l}]
  (if (str/blank? (str/trim txt))
    {:type :empty
     :block {:from {:pos pos :ln ln}
             :to {:pos (+ pos (count txt))}}
     :value nil}
    (let [[v rest] (do-read txt ln pos)]
      v)))
