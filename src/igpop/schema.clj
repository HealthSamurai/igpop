(ns igpop.schema)

(defn get-concepts [{valuesets :valuesets :as ctx} props]
  (if-let [vs (get props :valueset)]
    (let [vs' (get-in valuesets [(-> vs
                                     (get :id)
                                     keyword) :concepts])
          inlined-vs (get vs :concepts)
          prefixed-vs (get-in valuesets [(-> vs
                                             (get :id)
                                             (clojure.string/replace #"fhir:" "")
                                             keyword) :concepts])]
      (cond
        prefixed-vs
        (mapv #(get % :code) prefixed-vs)
        vs'
        (mapv #(get % :code) vs')
        inlined-vs
        (mapv #(get % :code) inlined-vs)))))

(defn get-required [els]
  (if-let [result (reduce (fn [acc [eln props]]
                            (conj acc (name eln))) [] (filter (fn [[eln props]] (or (:minItems props) (:required props))) els))]
    (when (> (count result) 0)
      result)))

(defn attach-required [acc eln props]
  (if-let [required-els (get-required props)]
    (assoc-in acc [eln :required] required-els)
    acc))

(defn attach-enum [acc eln props ctx]
  (if-let [concepts (get-concepts ctx props)]
    (assoc-in acc [eln :enum] concepts)
    acc))

(defn fhir-type-definition [type {definitions :definitions :as ctx}]
  (when-let [def (get definitions (keyword type))]
    def))

(defn make-ref [type prid]
  (str "#/definitions/" prid "/" type))

(defn attach-card-restrictions [acc eln props]
  (cond
    (and (:maxItems props) (:minItems props))
    (-> acc
        (assoc-in [eln :maxItems] (:maxItems props))
        (assoc-in [eln :minItems] (:minItems props)))
    (:maxItems props)
    (assoc-in acc [eln :maxItems] (:maxItems props))
    (:minItems props)
    (assoc-in acc [eln :minItems] (:minItems props))))

(defn attach-type [acc eln props]
  (cond
    (and (:type props) (:collection props))
    (-> acc
        (assoc-in [eln :items :type] (:type props))
        (assoc-in [eln :type] "array")
        (attach-card-restrictions eln props))
    (:union props)
    (assoc-in acc [eln :type] (vec (:union props)))
    (:type props)
    (assoc-in acc [eln :type] (:type props))
    (:collection props)
    (attach-card-restrictions (assoc-in acc [eln :type] "array") eln props)
    ))

(defn attach-description [acc eln props]
  (if-let [desc (:description props)]
    (assoc-in acc [eln :description] desc)
    acc))

(defn type-defintion [props]
  (when-let [t (keyword (:type props))]
    (let [t (if (= t :array)
              (-> props
                  (get-in [:items :type])
                  keyword)
              t)
          required (:required props)
          properties (:properties props)]
      (-> {}
          (attach-description t props)
          (cond-> required
            (assoc-in [t :required] required))
          (attach-required t props)
          (assoc-in [t :properties] properties)))))

(defn make-prid [rt prn]
  (str (name rt) (when-not (= :basic prn) (str "-" (name prn)))))

(defn element-to-schema [acc [eln props] ctx]
  (if (map? props)
    (let [acc' (-> acc
                   (attach-description eln props)
                   (attach-type eln props)
                   (attach-enum eln props ctx))]
      (if (:elements props)
        (-> acc'
            (attach-required eln (:elements props))
            (assoc-in [eln :properties] (reduce (fn [acc el] (element-to-schema acc el ctx)) acc (:elements props))))
        acc'))))

(defn profile-to-schema [rt prn props ctx]
  (assoc {} (keyword (str (name rt) (when (not (= "basic" (name prn)))
                                      (str "_" (name prn)))))
         (let [els (get props :elements)]
           (if-let [rqrd (get-required els)]
             (assoc {} :required rqrd :properties (into {} (map (fn [el] (element-to-schema {} el ctx)) els)))
             (assoc {} :properties (into {} (map (fn [el] (element-to-schema {} el ctx)) els)))))))

(defn generate-schema [{profiles :profiles :as ctx}]
  (let [m {:$schema "http://json-schema.org/draft-07/schema#"
           :$id (str "baseurl" "/" ".json")}]
    (assoc m :definitions
           (into {} (apply concat (for [[rt prls] profiles]
                                   (for [[prn props] prls]
                                     (assoc {} (keyword (str (name rt) (when (not (= "basic" (name prn)))
                                                                         (str "_" (name prn)))))
                                            (let [els (get props :elements)]
                                              (if-let [rqrd (get-required els)]
                                                (assoc {} :required rqrd :properties (into {} (map (fn [el] (element-to-schema {} el ctx)) els)))
                                                (assoc {} :properties (into {} (map (fn [el] (element-to-schema {} el ctx)) els)))))))))))))

