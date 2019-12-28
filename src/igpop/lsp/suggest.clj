(ns igpop.lsp.suggest
  (:require [clojure.string :as str]))

(def completion-item-kind {:Text 1
                           :Method 2
                           :Function 3
                           :Constructor 4
                           :Field 5
                           :Variable 6
                           :Class 7
                           :Interface 8
                           :Module 9
                           :Property 10
                           :Unit 11
                           :Value 12
                           :Enum 13
                           :Keyword 14
                           :Snippet 15
                           :Color 16
                           :File 17
                           :Reference 18
                           :Folder 19
                           :EnumMember 20
                           :Constant 21
                           :Struct 22
                           :Event 23
                           :Operator 24
                           :TypeParameter 25
                           })

(defn lsp-pos->ig-pos [{:keys [line character]}] {:ln line :pos character})
(defn parse-uri [uri] (-> uri
                          (str/split #"/")
                          (last)
                          (str/split #"\.")
                          (first)
                          (keyword)
                          ))


;; todo: compare pos
(defn in-block? [{{fln :ln fpos :pos} :from {tln :ln tpos :pos} :to :as block} {ln :ln pos :pos :as coord}]
  (when (and fln tln)
        (and
         (>= ln fln)
         (<= ln tln))))

(defn pos-to-path [{tp :type val :value :as ast} coord]
  (cond
    (= :map tp)
    (->> val
         (reduce (fn [acc {block :block :as kv}]
                   (if (in-block? block coord)
                     (let [res (pos-to-path (:value kv) coord)]
                       (into [(:key kv)] (or res [])))
                     acc))
                 nil))
    :else nil))

(defn node->suggests [node kind]
  (map (fn [[element-name val]]
         {:label (str (name element-name) ": ")
          :kind kind
          :detail (:description val)})
       node))


(def extension-elm {:extension {
                            :description "Additional content defined by implementations"
                            :type "Extension"
                            }})

(defn sgst-elements-name
  [ctx pth conent]
  (if (= (last pth) :elements)
    (let [base-profiles (get-in ctx [:manifest :base :profiles])
          elements (get-in base-profiles pth)]
      (node->suggests (conj elements extension-elm) (:Property completion-item-kind)))))


(defn sgst-igpop-keys
  [ctx pth conent]
  (let [igpop-schema (get-in ctx [:manifest :schema])]
    (loop [keys (rest pth)
           cur-ig-node igpop-schema]
      (if (empty? keys)
        (if-not (= (:type cur-ig-node) "Map")
          (node->suggests cur-ig-node (:Value completion-item-kind)))
        (let [[cur-key & rest] keys]
          (cond
            (cur-key cur-ig-node) (recur rest (cur-key cur-ig-node))
            (= (:type cur-ig-node) "Map") (recur rest (:value cur-ig-node))))))))


(defn collect
  ([ctx pth] (collect ctx pth nil))
  ([ctx pth content]
   (reduce (fn [acc suggester]
             (if-let [suggest (suggester ctx pth content)]
               (into acc suggest)
               acc))
           []
           [sgst-elements-name
            sgst-igpop-keys])))

(defn suggest [ctx msg ast]
  (let [params (:params msg)
        uri (get-in params [:textDocument :uri])
        rt (parse-uri uri)
        pos (lsp-pos->ig-pos (:position params))
        path (filterv keyword? (pos-to-path ast pos))
        suggests (collect ctx (into [rt] path))]
    (println "\n----------request_params---------  " (into [rt] path) " \n")
    #_(clojure.pprint/pprint suggests)
    (println "\n------------------------------\n")
    suggests))

(comment
  )
