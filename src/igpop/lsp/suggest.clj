(ns igpop.lsp.suggest)

(def sg
  {[:Patient] [:elements :description :api]
   [:Patient :elements] [:name :birthDate :identifier]
   [:Patient :elements :name] [:elements :minItems :maxItems]})


;; todo: compare pos
(defn in-block? [{{fln :ln fpos :pos} :from {tln :ln tpos :pos} :to :as block} {ln :ln pos :pos :as coord}]
  ;; (println block " ? " coord)
  (when (and fln tln)
        (and
         (>= ln fln)
         (<= ln tln))))

(defn pos-to-path [{tp :type val :value :as ast} coord]
  ;; (println "tp: " tp)
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

(defn *suggest [ctx pth]
  (get sg pth))

(defn suggest [ctx rt ast pos]
  (let [pth (into [rt] (filterv keyword? (pos-to-path ast pos)))]
    ;; (println "search " pth)
    (*suggest ctx pth)))
