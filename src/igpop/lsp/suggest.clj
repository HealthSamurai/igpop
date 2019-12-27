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

(defn sgst-elements-name
  [ctx pth cont]
  (if (= (last pth) :elements)
    (let [base-profiles (get-in ctx [:manifest :base :profiles])
          elements (get-in base-profiles pth)]
      (map (fn [[element-name val]]
             {:label (str (name element-name) ": ")
              :kind (:Property completion-item-kind)
              :detail (:description val)})
           elements))))


(defn collect
  ([ctx pth] (collect ctx pth nil))
  ([ctx pth content]
   (reduce (fn [acc suggester]
             (if-let [suggest (suggester ctx pth content)]
               (into acc suggest)
               acc))
           []
           [sgst-elements-name])))

;; {:jsonrpc 2.0, :id 6, :method textDocument/completion, :params {:textDocument {:uri file:///Users/mput/projects/igpop/example/src/DiagnosticReport/Patient.igpop}, :position {:line 4, :character 1}, :context {:triggerKind 1}}}

(defn suggest [ctx msg ast]
  (let [params (:params msg)
        uri (get-in params [:textDocument :uri])
        rt (parse-uri uri)
        pos (lsp-pos->ig-pos (:position params))
        path (filterv keyword? (pos-to-path ast pos))
        suggests (collect ctx (into [rt] path))]
    (println "\n----------suggest_res---------\n" rt path)
    (println "\n------------------------------\n")
    suggests))