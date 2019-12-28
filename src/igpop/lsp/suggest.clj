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
         {:label (str (name element-name) ":")
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
      (node->suggests (into (or elements []) extension-elm) (:Property completion-item-kind)))))



(defn sgst-igpop-keys
  [ctx pth conent]
  (let [igpop-schema (get-in ctx [:manifest :schema])]
    (loop [keys (rest pth)
           cur-ig-node igpop-schema]
      (if (empty? keys)
        (if-not (:Type cur-ig-node)
          (node->suggests cur-ig-node (:Value completion-item-kind)))
        (let [[cur-key & rest] keys]
          (cond
            (cur-key cur-ig-node) (recur rest (if-let [ref (:ref (cur-key cur-ig-node))]
                                                (get-in igpop-schema [(keyword ref)]) ;; need to process complex ref
                                                (cur-key cur-ig-node)))
            (= (:Type cur-ig-node) "Map") (recur rest (:value cur-ig-node))))))))


(defn sgst-hardcoded [_ pth _]
  (map (fn [val] {:label val :kind (:Enum completion-item-kind)})
       (let [last-kv? (fn [k] (= (last pth) k))]
        (cond-> []
          (last-kv? :minItems) (into ["1" "0"])
          (last-kv? :maxItems) (into ["*"])
          (last-kv? :required) (into ["true"])
          (last-kv? :disabled) (into ["true"])
          (last-kv? :description) (into ["| "])
          ))))

(defn collect
  ([ctx pth] (collect ctx pth nil))
  ([ctx pth content]
   (reduce (fn [acc suggester]
             (if-let [suggest (suggester ctx pth content)]
               (into acc suggest)
               acc))
           []
           [sgst-hardcoded
            sgst-elements-name
            sgst-igpop-keys])))

(defn suggest [ctx msg ast]
  (let [params (:params msg)
        uri (get-in params [:textDocument :uri])
        rt (parse-uri uri)
        pos (lsp-pos->ig-pos (:position params))
        path (filterv keyword? (pos-to-path ast pos))
        suggests (collect ctx (into [rt] path))]
    (println "\n----------request_params---------  " (into [rt] path) " \n")
    (clojure.pprint/pprint suggests)
    (println "\n------------------------------\n")
    suggests))

(defn igpop-key-doc [k]
  (get {:maxItems "Max items in collection collection (positive int)"
        :minItems "Min items in collection (positive int)"
        :elements "elements map element-name: element-spec"
        :valueset "Binding to valuset {id: <vs-id>}"} k))

(defn capital? [t]
  (and (string? t)
       (= (subs t 0 1) (str/upper-case (subs t 0 1)))))

(defn element-from-path [manif rt pth]
  (when-let [res-def (get-in manif [:base :profiles rt])]
    (loop [[p & ps :as aps] pth
           subj res-def]
      (if (nil? p)
        subj
        (when-let [subj' (get subj p)]
          (if (empty? ps)
            subj'
            (recur ps (if-let [tp (when-let [t (:type subj')]
                                    (when (capital? t) t))]
                        (get-in manif [:base :profiles (keyword tp)])
                        subj'))))))))

(defn element-doc [manif rt pth]
  (when-let [el (element-from-path manif rt pth)]
    (str
     (:description el)
     (when-let [tp (get-in el [:type])]
       (str " **" tp (when (:collection el) "[]") "**")))))

(defn hover [ctx msg ast]
  (let [params (:params msg)
        uri (get-in params [:textDocument :uri])
        rt (parse-uri uri)
        pos (lsp-pos->ig-pos (:position params))
        path (filterv keyword? (pos-to-path ast pos))
        doc (cond
              (= :elements (last (butlast (butlast path))))
              (igpop-key-doc (last path))
              :else
              (element-doc (:manifest ctx) rt path))]
    
    (println "Hover for" (into [rt] path))
    {:contents (if doc [doc] nil)}))

(comment
  )
