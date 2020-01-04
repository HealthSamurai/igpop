(ns igpop.schema
  (:require [flatland.ordered.map :refer :all]))

(defn dissoc-in [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn keys-in [m]
  (letfn [(children [node]
            (let [v (get-in m node)]
              (if (map? v)
                (map (fn [x] (conj node x)) (keys v))
                [])))
          (branch? [node] (-> (children node) seq boolean))]
    (->> (keys m)
         (map vector)
         (mapcat #(tree-seq branch? children %)))))

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

(defn cast-to-ordered-map [m eln]
  (update-in m [eln] ordered-map))

(defn attach-card-restrictions [acc eln props]
  (let [with-restrictions (cond
                            (and (:maxItems props) (:minItems props))
                            (-> acc
                                (assoc-in [eln :maxItems] (:maxItems props))
                                (assoc-in [eln :minItems] (:minItems props)))
                            (:maxItems props)
                            (assoc-in acc [eln :maxItems] (:maxItems props))
                            (:minItems props)
                            (assoc-in acc [eln :minItems] (:minItems props))
                            :else
                            acc)]
    (cast-to-ordered-map with-restrictions eln)))

(defn attach-type [acc eln props]
  (let [with-type (cond
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
                    (attach-card-restrictions (assoc-in acc [eln :type] "array") eln props))]
    (if with-type
      (cast-to-ordered-map with-type eln)
      acc)))

(defn attach-description [acc eln props]
  (if-let [desc (:description props)]
    (cast-to-ordered-map (assoc-in acc [eln :description] desc) eln)
    acc))

(defn make-prid [profile]
  (-> profile keys first name str))

(defn attach-prid [prid type]
  (keyword (str (name prid) "-" (name type))))

(defn get-type-pths [profile]
  (let [paths (filter (fn [el] (some #(= % :type) el)) (keys-in profile))]
    paths))

(defn replace-props [profile definitions ctx]
  (let [paths (get-type-pths profile)
        profile-name (make-prid profile)]
    (apply merge (for [pth paths]
                   (let [t (keyword (get-in profile pth))
                         prid (attach-prid profile-name t)
                         pth (-> pth butlast vec)]
                     (when (or (contains? definitions t) (contains? definitions prid))
                       (-> profile
                           (update-in pth clojure.set/rename-keys {:type :$ref})
                           (dissoc-in (conj (if (= :items (last pth))
                                              (butlast pth)
                                              pth) :properties)))))))))

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



(defn get-fhir-complex-def [type {{complex :complex} :definitions :as ctx}]
  (when-let [def (get complex (keyword type))]
    def))

(defn get-fhir-primitive-def [type {{primitive :primitive} :definitions :as ctx}]
  (when-let [def (get primitive (keyword type))]
    def))

(defn make-ref [type prid]
  (str "#/definitions/" prid "/" type))

(defn enrich-element-def [element-def ctx]
  (let [name-with-prid (-> element-def
                           keys
                           first)
        name-without-prid (-> name-with-prid
                              name
                              (clojure.string/split #"-")
                              last
                              keyword)
        fhir-def (get-fhir-complex-def name-without-prid ctx)]
    (->
     (ordered-map {})
     (assoc-in [name-with-prid] (merge (dissoc fhir-def :properties) (dissoc (get element-def name-with-prid) :properties)))
     (assoc-in [name-with-prid :properties] (merge (:properties fhir-def) (:properties (get element-def name-with-prid)))))))

(defn cut-fhir-type [pth]
  (-> pth
      (clojure.string/split #"/")
      last
      keyword))

(defn extract-refs [acc [eln props]]
  (let [acc (if-let [reference (get props :$ref)]
              (conj acc (cut-fhir-type reference))
              acc)]
    (if (:properties props)
      (reduce (fn [acc el]
                (extract-refs acc el)) acc (:properties props))
      acc)))

(defn get-refered-def [props ctx]
  (let [refered-types (reverse (set (extract-refs [] (first (seq props)))))]
    (mapv (fn [el]
            (if-let [def (get-fhir-complex-def el ctx)]
              (assoc (ordered-map {}) el def)
              (assoc (ordered-map {}) el (get-fhir-primitive-def el ctx)))) refered-types)))

(defn extract-element-def [props ctx prid]
  (let [t (if (= (keyword (:type props)) :array)
            (-> props
                (get-in [:items :type])
                keyword)
            (keyword (:type props)))
        fhir-def (get-fhir-complex-def t ctx)
        properties (:properties props)]
    (cond
      (and properties fhir-def)
      (let [t' (attach-prid prid t)]
        (assoc {} t' (-> props
                         (dissoc :items)
                         (dissoc :type))))
      fhir-def
      (assoc {} t fhir-def))))

(defn extract-simple-types [props ctx definitions]
  (let [paths (get-type-pths props)]
    (into (empty definitions) (concat definitions (for [pth paths]
                                                    (let [t (-> props (get-in pth) keyword)]
                                                      (when (not (contains? definitions t))
                                                        (if-let [primitive (get-fhir-primitive-def t ctx)]
                                                          (assoc {} t primitive)))))))))

(defn shape-up-definitions [pr-schema ctx]
  (let [profile-name (-> pr-schema keys first)
        props (get-in pr-schema [profile-name :properties])
        definitions (reduce (fn [acc el]
                              (if-let [def (extract-element-def (val el) ctx (make-prid pr-schema))]
                                (let [enriched-def (enrich-element-def def ctx)
                                      k (first (keys enriched-def))
                                      v (get enriched-def k)]
                                  (vec (concat acc (conj (get-refered-def {k v} ctx) {k v}))))
                                acc)) [] props)]
    (extract-simple-types pr-schema ctx definitions)))

(defn profile-to-schema [rt prn props ctx]
  (let [pr-schema (assoc (ordered-map {}) (keyword (str (name rt) (when (not (= "basic" (name prn)))
                                                                    (str "_" (name prn)))))
                         (let [els (get props :elements)
                               properties (assoc (ordered-map {}) :properties (ordered-map (into {} (map (fn [el] (element-to-schema (ordered-map {}) el ctx)) els))))]
                           (if-let [required-elements (get-required els)]
                             (assoc properties :required required-elements))))
        definitions (ordered-map (into {} (shape-up-definitions pr-schema ctx)))]
    (assoc {} :definitions (conj definitions pr-schema))))

;;deprecated
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
