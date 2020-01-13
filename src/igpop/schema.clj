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

(defn cast-to-ordered-map [m eln]
  (update-in m [eln] ordered-map))

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
                    (assoc-in acc [eln :type] (:type props)))]
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

(defn make-ref
  ([type]
   (str "#/definitions/" type))
  ([type prid]
   (str "#/definitions/" prid "-" type)))

(defn get-fhir-complex-def [type {{complex :complex} :definitions :as ctx}]
  (when-let [def (get complex (keyword type))]
    def))

(defn get-fhir-primitive-def [type {{primitive :primitive} :definitions :as ctx}]
  (when-let [def (get primitive (keyword type))]
    def))

(defn replace-props [profile definitions ctx]
  (let [paths (filter #(and (not (coll? (get-in profile %))) (not (= (nth % 3) :properties))) (get-type-pths profile))
        profile-name (make-prid profile)]
    (loop [profile profile
           paths paths]
      (if (not paths)
        profile
        (recur
         (let [t (keyword (get-in profile (first paths)))
               prid (attach-prid profile-name t)
               pth (-> (first paths) butlast vec)]
           (if (or (contains? definitions t) (contains? definitions prid))
             (-> profile
                 (update-in pth clojure.set/rename-keys {:type :$ref})
                 (assoc-in (conj pth :$ref) (if (and (get-in profile (conj (vec (if (= :items (last pth))
                                                                                  (butlast pth)
                                                                                  pth)) :properties)) (get-fhir-complex-def t ctx) (not (= t :array)))
                                              (make-ref (name t) profile-name)
                                              (make-ref (name t))))
                 (dissoc-in (conj (vec (if (= :items (last pth))
                                         (butlast pth)
                                         pth)) :properties)))
             profile))
         (next paths))))))

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

(defn get-refered [props ctx]
  (let [refered-types (reverse (set (extract-refs [] (first (seq props)))))]
    (mapv (fn [el]
            (if-let [def (get-fhir-complex-def el ctx)]
              (assoc (ordered-map {}) el def)
              (assoc (ordered-map {}) el (get-fhir-primitive-def el ctx)))) refered-types)))

(defn extract-element-def [props ctx prid]
  (when (:properties props)
    (let [t (cond
              (= (keyword (:type props)) :array)
              (-> props
                  (get-in [:items :type])
                  keyword)
              (coll? (:type props))
              (map #(keyword %) (:type props))
              :else
              (keyword (:type props)))#_(if (= (keyword (:type props)) :array)
                                          (-> props
                                              (get-in [:items :type])
                                              keyword)
                                          (keyword (:type props)))
          fhir-def (if (coll? t)
                     (map #(get-fhir-complex-def % ctx) t)
                     (get-fhir-complex-def t ctx))
          properties (:properties props)]
      (cond
        (and (not (coll? t)) properties fhir-def)
        (let [t' (attach-prid prid t)]
          (assoc {} t' (-> props
                           (dissoc :items)
                           (dissoc :type))))
        fhir-def
        (if (coll? t)
          (map #(assoc {} % fhir-def) t)
          (assoc {} t fhir-def))))))

(defn get-def [t ctx definitions]
  (when (and t) (not (contains? definitions t))
        (if-let [primitive (get-fhir-primitive-def t ctx)]
          (assoc {} t primitive)
          (if-let [complex (get-fhir-complex-def t ctx)]
            (assoc {} t complex)))))

(defn process-unions [props ctx definitions]
  (let [paths (filter #(coll? (get-in props %)) (get-type-pths props))]
    (apply (comp distinct concat) (for [pth paths]
                                    (let [t (get-in props pth)]
                                      (apply (comp distinct concat) (map (fn [el]
                                                                           (let [def (get-def el ctx definitions)
                                                                                 refered (get-refered def ctx)]
                                                                             (conj refered def))) t)))))))

(defn extract-fhir-types [props ctx definitions]
  (let [paths (get-type-pths props)
        unions (process-unions props ctx definitions)]
    (into (empty definitions) (concat unions definitions (for [pth paths]
                                                    (let [t (-> props (get-in pth) keyword)]
                                                      (when (not (contains? (into {} definitions) (attach-prid (first pth) t)))
                                                        (get-def t ctx definitions))))))))

(defn shape-up-definitions [pr-schema ctx]
  (let [profile-name (-> pr-schema keys first)
        props (get-in pr-schema [profile-name :properties])
        definitions (reduce (fn [acc el]
                              (if-let [def (extract-element-def (val el) ctx (make-prid pr-schema))]
                                (let [enriched-def (enrich-element-def def ctx)
                                      k (first (keys enriched-def))
                                      v (get enriched-def k)]
                                  (vec (into acc (conj (get-refered {k v} ctx) {k v}))))
                                acc)) [] props)]
    (extract-fhir-types pr-schema ctx definitions)))

(defn profile-to-schema [rt prn props ctx]
  (let [pr-schema (assoc (ordered-map {}) (keyword (str (name rt) (when (not (= "basic" (name prn)))
                                                                    (str "_" (name prn)))))
                         (let [base {:resourceType {:description (str "This is a " (name rt) " resource")
                                                    :const (name rt)}
                                     :id (make-ref "id")}
                               els (get props :elements)
                               properties (assoc (ordered-map {})
                                                 :description (:description props)
                                                 :properties (ordered-map (into base (map (fn [el] (element-to-schema (ordered-map {}) el ctx)) els))))]
                           (if-let [required-elements (get-required els)]
                             (assoc properties :required required-elements))))
        definitions (assoc (ordered-map (into {} (shape-up-definitions pr-schema ctx))) :id (get-fhir-primitive-def :id ctx))]
    (assoc {} :definitions (conj definitions (replace-props pr-schema definitions ctx)))))
