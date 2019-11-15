(ns igpop.schema)

(defn get-concepts [{valuesets :valuesets :as ctx} valueset]
  (when-let [vs (get-in valuesets [valueset :concepts])]
    (mapv #(get % :code) vs)))

(defn attach-enum [acc eln vs ctx]
  (if vs
    (assoc-in acc [eln :enum] (get-concepts ctx vs))
    acc))

(defn element-to-json-schema [acc [eln props] ctx]
  (if (map? props)
    (let [acc' (-> acc
                   (assoc-in [eln :decription] (:description props))
                   (assoc-in [eln :type] (:type props))
                   (attach-enum eln (keyword (get-in props [:valueset :id])) ctx))]
      (if (:elements props)
        (assoc-in acc' [eln :properties] (reduce (fn [acc el] (element-to-json-schema acc el ctx)) acc (:elements props)))
        acc'))))

(defn generate-json-schema [[rt prls] ctx]
  (assoc {} rt
         (into {} (for [[prn props] prls]
                   {prn (let [prl-sch {:$schema "http://json-schema.org/draft-07/schema#"
                                       :$id (str "baseurl" "/"(name rt) (name prn) ".json")}
                              els (get props :elements)]
                          (assoc prl-sch :definitions (into {} (map (fn [el] (element-to-json-schema {} el ctx)) els))))}))))
