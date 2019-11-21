(ns igpop.schema)

(defn get-concepts [{valuesets :valuesets :as ctx} valueset]
  (when-let [vs (get-in valuesets [valueset :concepts])]
    (mapv #(get % :code) vs)))

(defn get-required [els]
  (if-let [result (reduce (fn [acc [eln props]]
                            (conj acc (name eln))) [] (filter (fn [[eln props]] (or (:minItems props) (:required props))) els))]
    result
    []))

(defn attach-enum [acc eln vs ctx]
  (if vs
    (let [vs' (get-concepts ctx (keyword vs))
          vs'' (get-concepts ctx (-> vs
                                     (clojure.string/replace #"fhir:" "")
                                     keyword))]
      (cond
        vs'
        (assoc-in acc [eln :enum] vs')
        vs''
        (assoc-in acc [eln :enum] vs'')
        :else acc))
    acc))

(defn attach-type [acc eln props]
  (if-let [types (:union props)]
    (assoc-in acc [eln :type] (vec types))
    (assoc-in acc [eln :type] (:type props))))

(defn element-to-schema [acc [eln props] ctx]
  (if (map? props)
    (let [acc' (-> acc
                   (assoc-in [eln :decription] (:description props))
                   (attach-type eln props)
                   (attach-enum eln (-> props
                                        (get-in [:valueset :id])) ctx))]
      (if (:elements props)
        (-> acc'
            (assoc-in [eln :required] (get-required (:elements props)))
            (assoc-in [eln :properties] (reduce (fn [acc el] (element-to-schema acc el ctx)) acc (:elements props))))
        acc'))))

(defn generate-schema [{profiles :profiles :as ctx}]
  (let [m {:$schema "http://json-schema.org/draft-07/schema#"
           :$id (str "baseurl" "/" ".json")}]
    (assoc m :definitions
           (into {} (apply concat (for [[rt prls] profiles]
                                   (for [[prn props] prls]
                                     (assoc {} (keyword (str (name rt) "-" (name prn))) (let [els (get props :elements)]
                                                                                          (assoc {} :required (get-required els)
                                                                                                       :properties (into {} (map (fn [el] (element-to-schema {} el ctx)) els))))))))))))
