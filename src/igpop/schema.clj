(ns igpop.schema)

(defn get-concepts [{valuesets :valuesets :as ctx} valueset]
  (when-let [vs (get-in valuesets [valueset :concepts])]
    (mapv #(get % :code) vs)))

(defn get-required [els]
  (if-let [r (reduce (fn [acc [eln props]]
                       (if (:required props)
                         (conj acc (name eln)))) [] els)]
    r
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
  (if-let [l (:union props)]
    (assoc-in acc [eln :type] (vec l))
    (assoc-in acc [eln :type] (:type props))))

(defn element-to-json-schema [acc [eln props] ctx]
  (if (map? props)
    (let [acc' (-> acc
                   (assoc-in [eln :decription] (:description props))
                   (attach-type eln props)
                   (attach-enum eln (-> props
                                        (get-in [:valueset :id])) ctx))]
      (if (:elements props)
        (-> acc'
            (assoc-in [eln :required] (get-required (:elements props)))
            (assoc-in [eln :properties] (reduce (fn [acc el] (element-to-json-schema acc el ctx)) acc (:elements props))))
        acc'))))

(defn generate-json-schema [{profiles :profiles :as ctx}]
  (let [m {:$schema "http://json-schema.org/draft-07/schema#"
           :$id (str "baseurl" "/" ".json")}]
    (assoc m :definitions
           (into {} (for [[rt prls] profiles]
                      (assoc {} rt
                             (into {} (for [[prn props] prls]
                                        {prn (let [els (get props :elements)]
                                               (assoc-in {} [:properties] (into {} (map (fn [el] (element-to-json-schema {} el ctx)) els))))}))))))))

