(ns igpop.schema)

(defn get-concepts [ctx vs]
  )

(defn element-to-json-schema [acc [eln props]]
  (if (map? props)
    (if (:elements props)
      (assoc-in (-> acc
                    (assoc-in [eln :decription] (:description props))
                    (assoc-in [eln :type] (:type props))) [eln :properties] (reduce element-to-json-schema acc (:elements props)))
      (-> acc
          (assoc-in [eln :decription] (:description props))
          (assoc-in [eln :type] (:type props))))))

(defn generate-json-schema [[rt prls] ctx]
  (assoc {} rt
         (into {} (for [[prn props] prls]
                   {prn (let [prl-sch {:$schema "http://json-schema.org/draft-07/schema#"
                                       :$id (str "baseurl" "/"(name rt) (name prn) ".json")}
                              els (get props :elements)]
                          (assoc prl-sch :definitions (into {} (map #(element-to-json-schema {} %) els))))}))))

