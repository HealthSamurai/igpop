(ns yaml-ast.core
  (:require [clojure.string :as str]
            [zprint.core :as zp]))

(defn reader [s]
  (atom {:src s :pos 0 :len (count s)}))

(defn readc [r]
  (let [{pos :pos s :src l :len} @r
        npos (inc pos)]
    (if (>= npos l)
      :eof
      (do
        (swap! r assoc :pos npos)
        (subs (:src @r) pos npos)))))


(defn str-at [inp pos]
  (subs inp pos (inc pos)))

(def state-machine
  {:start {"{" :obj
           "[" :coll
           "'" :str}
   :obj {"}" :start}
   :coll {"]" :start}})

(defn read-string [inp pos]
  (if (>= pos (count inp))
    {:type :eof
     :error "Expected string got eof"
     :start pos
     :pos pos}
    (let [ch (str-at inp pos)]
      (if (= "'" ch)
        (let [len (count inp)]
          (loop [pos-i (inc pos)]
            (if (>= pos-i len)
              {:value (subs inp (inc pos) pos-i)
               :type :str
               :start pos
               :pos pos-i
               :error "Unterminated string"}
              (if (= "'" (str-at inp pos-i))
                {:value (subs inp (inc pos) pos-i)
                 :type :str
                 :start pos
                 :pos (inc pos-i)}
                (recur (inc pos-i))))))
        {:type :unknown
         :error (str "Expected ' got " ch)
         :start pos
         :pos pos}))))


(defn mk-read-regex [tp regex]
  (fn [inp pos]
    (if-let [v (re-find regex (subs inp pos))]
      {:type tp
       :value v
       :start pos
       :pos (+ pos (count v))}
      {:type tp
       :error (str "Does not match " regex)
       :start pos
       :pos pos})))

(def read-spaces (mk-read-regex :spaces #"^\s*"))

(re-find #"^([a-zA-Z0-9]+)(:)?" "-mykey: ups")

(def key-regex #"^([a-zA-Z0-9]+)(:)?")

(defn read-key [inp pos]
  (if-let [[_ key col] (re-find key-regex (subs inp pos))]
    (let [len (count key)]
      (if col
        {:value key
         :type :key
         :start pos
         :pos (+ pos (inc len))}
        {:value key
         :type :err-key
         :error "expected :"
         :start pos
         :pos (+ pos len)}))
    {:type :empty-key
     :start pos
     :pos pos
     :error "expected key"}))

(defn read-comma? [inp pos]
  (when-let [comma (re-find #"^\s*," (subs inp pos))]
    {:pos (+ pos (count comma))}))

(defn read-map-end? [inp pos]
  (when-let [subj (re-find #"^\s*}" (subs inp pos))]
    {:pos (+ pos (count subj))}))

(defn read-closing-curl [inp pos]
  (if-let [curl-idx (str/index-of inp "}" pos)]
    {:value (subs inp pos curl-idx)
     :pos (+ pos (inc curl-idx))}
    {:value (subs inp pos)
     :pos (+ pos (count inp))}))

(declare read-map)

(defn read-value [inp pos]
  (let [{pos :pos} (read-spaces inp pos)]
    (if (str/starts-with? (subs inp pos) "{")
      (read-map inp pos)
      (read-string inp pos))))

(defn read-map [inp opos]
  (let [pos (inc opos)]
    (loop [entry []
           npos pos]
      (let [{pos-0 :pos} (read-spaces inp npos)
            {tp :tp pos-1 :pos :as key} (read-key inp pos-0)]
        (if-let [{pos-2 :pos} (read-comma? inp pos-1)]
          (let [entry (conj entry {:key key :start npos :pos pos-2})]
            (recur entry pos-2))
          (if-let [{pos-2 :pos} (read-map-end? inp pos-1)]
            {:type :map
             :value (conj entry {:key key :start npos :pos pos-2})
             :start opos :pos pos-2}
            (let [{tp :tp err :error pos-2 :pos :as val} (read-value inp pos-1)
                  entry (conj entry {:key key :value val :start npos :pos pos-2})]
              (if-let [{pos-3 :pos} (read-comma? inp pos-2)]
                (recur entry pos-3)
                (if-let [{pos-3 :pos} (read-map-end? inp pos-2)]
                  {:type :map :value entry :start opos :pos pos-3}
                  (let [{tp :tp pos-3 :pos} (read-closing-curl inp pos-2)]
                    {:type :map
                     :value entry
                     :error (str "extra content in map - " (subs inp pos-2 (min (count inp) pos-3)))
                     :pos pos-3}))))))))))


;; (read-keyval "mykey:'sss'" 0)
;; (read-keyval "mykey: 'sss'" 0)

;; (read-string  "'abcdefg'" 0)
;; (read-string  "'a'" 0)

;; (read-string  "abcdefg'" 0)
;; (read-string  "'abcdefg" 0)
;; (read-key "mykey: a" 0)
;; (read-key "mykey a" 0)

;; (read-closing-curl "ooo} bbb" 0)
;; (read-value "  'a'" 0)

(zprint.core/zprint
 (read-map "{a: 'b', c: 'd'}" 0))

(zprint.core/zprint
 (read-map "{a: 'b', c}" 0))


(zprint.core/zprint
 (read-map "{a: 'b', c: {d: 'd', e: 'f' } }" 0))

(zprint.core/zprint
 (read-map "{a, b: {c: 'd'}}" 0))
