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
         :ident ident
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

(defn parse-collection [lines opts]
  {:type :coll})

(defn parse-inline [{ln :ln txt :text pos :pos :as l}]
  {:type :str
   :value (str/trim txt)
   :block {:from {:ln ln :pos pos}
           :to {:ln ln :pos (+ pos (count txt))}}})

(declare parse-block)

(defn parse-map [lines opts]
  (let [value (loop [[{txt :text :as ln} & lns] lines
                      value []
                      cur-val nil]
                 (if (nil? ln)
                   (if cur-val
                     (conj value (let [lines (:lines cur-val)
                                       val (if-not (empty? lines)
                                             (parse-block lines opts)
                                             {:type :null})]
                                   (-> (dissoc cur-val :lines)
                                       (assoc :value val)
                                       (assoc-in [:block :to] (get-in val [:block :to])))))
                     value)
                   (if-not (= 0 (:ident ln))
                     ;; TODO: check acc
                     (if (>= (:ident ln) 2)
                       (recur lns value (update cur-val :lines conj (update ln :ident #(- % 2))))
                       (recur lns value cur-val))
                     (if (str/blank? (str/trim txt))
                       (recur lns value cur-val)
                       (let [ value' (if cur-val
                                         (conj value (let [lines (:lines cur-val)
                                                           val (parse-block lines opts)]
                                                       (-> (dissoc cur-val :lines)
                                                           (assoc :value val)
                                                           (assoc-in [:block :to] (get-in val [:block :to])))))
                                         value)]
                         (if-let [[kk k] (re-find key-regex txt)]
                           (let [kk-len (count kk)
                                 k (keyword k)
                                 lrest (subs txt kk-len)
                                 from {:ln (:ln ln) :pos (:pos ln)}]
                             (if-not (str/blank? lrest)
                               (recur lns
                                      (conj value' {:type :kv
                                                    :block {:from from
                                                            :to {:ln (:ln ln) :pos (+ (:pos ln) (count (:text ln)))}}
                                                    :key k
                                                    :value (parse-inline {:text lrest :ln (:ln ln) :ident 0 :pos (+ (:pos ln) kk-len)})})
                                      nil)
                               (let [cur-val' {:type :kv
                                               :key k
                                               :block {:from from}
                                               :lines []}]
                                 (recur lns value' cur-val'))))
                           (recur lns (conj value' {:type :kv
                                                    :block {:from {:ln (:ln ln) :pos (:pos ln)}
                                                            :to {:ln (:ln ln) :pos (+ (:pos ln) (count txt))}}
                                                    :invalid true
                                                    :key (str/trim txt)})
                                  nil)))))))]
    {:type :map
     :block {:from (get-in (first value) [:block :from])
             :to  (get-in (last value) [:block :to])}
     :value value}))

(defn parse-block [lines opts]
  (if (= :map (:start opts))
    (parse-map lines (dissoc opts :start))
    (let [type (block-type lines)]
      (cond
        (= :coll type) (parse-collection lines opts)
        (= :map type) (parse-map lines opts)
        :else (parse-map lines opts)))))

(defn parse [s & [opts]]
  (parse-block (parse-lines s) opts))


