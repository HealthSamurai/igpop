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


(defn block-type [[{txt :text} & lns]]
  (cond
    (str/starts-with? txt "-") :coll
    (re-find key-regex  txt) :map
    :else :unknown))

(defn parse-collection [lines]
  {:type :coll})

(defn parse-inline [{ln :ln txt :text pos :pos :as l}]
  {:type :str
   :value txt
   :block {:from {:ln ln :pos pos}
           :to {:ln ln :pos (+ pos (count txt))}}})

(declare parse-block)

(defn parse-map [lines]
  (let [groups (loop [[{txt :text :as ln} & lns] lines
                      cur-key nil
                      cur-grp nil
                      grps []]
                 (println ln)
                 (if (nil? ln)
                   (if cur-key
                     (conj grps
                           {:type :kv
                            :key cur-key
                            :block {:from {} :to {}}
                            :value (if (= 1 (count cur-grp))
                                     (parse-inline (first cur-grp))
                                     (parse-block cur-grp))})
                     grps)
                   (if (= 0 (:ident ln))
                     (let [[kk k] (re-find key-regex txt)
                           kk-len (count kk)
                           k (keyword k)
                           lrest (str/trim (subs txt kk-len))
                           cur-grp' (if (str/blank? lrest) [] [{:text lrest
                                                                :ln (:ln ln)
                                                                :ident 0
                                                                :pos (+ (:pos ln) kk-len)}])]
                       (if cur-key
                         (recur lns k cur-grp' (conj grps
                                                     {:type :kv
                                                      :key cur-key
                                                      :value (if (= 1 (count cur-grp))
                                                               (parse-inline (first cur-grp))
                                                               (parse-block cur-grp))}))
                         (recur lns k cur-grp' grps)))
                     (recur lns cur-key (conj cur-grp (update ln :ident #(- % 2))) grps))))]
    {:type :map
     :block {:from (let [fl (first lines)]
                     {:ln (:ln fl) :pos (:pos fl)})
             :to (let [ll (last lines)]
                   {:ln (:ln ll) :pos (+ (:pos ll) (count (:text ll)))})}
     :value groups}))

(defn parse-block [lines]
  (let [type (block-type lines)]
    (cond
      (= :coll type) (parse-collection lines)
      (= :map type) (parse-map lines)
      :else (assert false "TODO"))))

(defn parse [s]
  (parse-block (parse-lines s)))


