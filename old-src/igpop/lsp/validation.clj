(ns igpop.lsp.validation
  (:require [igpop.parser]
            [clojure.string :as str]))


(defn capital? [t]
  (and (string? t)
       (= (subs t 0 1) (str/upper-case (subs t 0 1)))))


(defn validate-value [errs manifest meta-cursor path value]
  (cond
    (nil? meta-cursor) errs

    (and (map? value) (:elements value))
    (reduce (fn [errs [k v]]
              (if-let [el (get-in meta-cursor [:elements k])]
                (let [meta-cur (if-let [tp (:type el)]
                                 (if (and (string? tp) (capital? tp))
                                   (get manifest (keyword tp))
                                   el)
                                 el)]
                  (validate-value errs manifest meta-cur (into path [:elements k]) v))
                (conj errs {:path (into path [:elements k]) :message (str "Unknown key - " (name k) ". Expected one of: " (str/join ", " (sort (mapv name (keys (:elements meta-cursor))))))})
                )) errs (dissoc (:elements value) :extension))
    :else errs))

(defn validate-profile [ctx rt value]
  (let [manifest (get-in ctx [:manifest :base :profiles])
        meta-cur     (get manifest rt)]
    (validate-value [] manifest meta-cur [] value)))

(defn collect-value* [acc pth ast]
  (cond
    (= :map (:type ast))
    (reduce (fn [acc kv]
              (if-let [k (:key kv)]
                (let [prev-value (:value acc)
                      new-pth (conj pth k)]
                  (if (and (keyword? k))
                    (let [acc' (collect-value* (dissoc acc :value) new-pth (:value kv))]
                      (-> acc'
                          (assoc-in [:idx new-pth] (let [{frm :from} (:block kv)]
                                                     {:from frm
                                                      :to {:ln (:ln frm) :pos (+ (:pos frm) (count (name k)))}}))
                          (assoc :value (assoc prev-value k (:value acc')))))
                    acc))
                acc)
              ) acc (:value ast))
    :else (-> (assoc acc :value (:value ast))
              (assoc-in [:idx pth] (:block ast)))))

(defn collect-value [ast]
  (collect-value* {:value {} :idx {}} [] ast))

(defn block-to-position [{from :from to :to :as blk}]
  (when blk
    {:start {:line (:ln from) :character (:pos from)}
     :end {:line (:ln to) :character (:pos to)}}))

(defn errors-to-diagnostic [idx errs]
  (reduce (fn [diags {pth :path msg :message}]
            (if-let [block (get idx pth)]
              (conj diags
               {:range (block-to-position block) 
                :message msg})
              diags)) [] errs))

(defn structure-validation [ctx rt ast]
  (let [{idx :idx value :value} (collect-value ast)
        errs (validate-profile ctx rt value)
        diag (errors-to-diagnostic idx errs)]
    (println "VALIDATION"
             "VAL:" value "\n"
             "ERR:" diag)
    diag))


(defn resource-type-from-uri [uri] (when uri (-> uri (str/split #"/") (last) (str/split #"\.") (first) (keyword))))

(defn validate [ctx doc]
  (let [ast (:ast doc)
        errors (igpop.parser/errors ast)
        uri (get-in doc [:params :textDocument :uri])
        rt (resource-type-from-uri uri)
        syntax-diags (->> errors
                         (mapv (fn [{block :block msg :message}]
                                 {:range (block-to-position block)
                                  :message msg})))
        diags (structure-validation ctx rt ast)]
    {:method "textDocument/publishDiagnostics"
     ;; todo doc should be params
     :params {:uri uri
              :diagnostics (into syntax-diags diags)}}))
