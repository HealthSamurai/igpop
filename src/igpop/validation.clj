(ns igpop.validation
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.set]))

(defn basic-schema []
  (yaml/parse-string
   (slurp (.getPath (io/resource "loll/schema.yaml")))
   :keywords true))

(defmacro reduce-with [acc coll f]
  `(reduce ~f ~acc ~coll))

(defn add-error
  ([v-ctx msg]
   (update v-ctx :errors conj {:path (:path v-ctx) :message msg}))
  ([v-ctx k msg]
   (update v-ctx :errors conj {:path (conj (:path v-ctx) k) :message msg})))

(defn validate-impl [ctx {pth :path :as v-ctx} schema subj]
  (if (and (:req schema) (nil? subj))
    (add-error v-ctx (str "Element is required"))
    (if-let [attrs (:attrs schema)]
      (let [o-keys (into #{} (keys subj))
            s-keys (into #{} (keys attrs))
            unknowns (clojure.set/difference o-keys s-keys)
            v-ctx (if-not (empty? unknowns)
                    (reduce-with v-ctx unknowns
                                 (fn [acc k]
                                   (add-error acc k "Unknown element")))
                    v-ctx)]
        (reduce-with
         v-ctx attrs
         (fn [v-ctx [k sch]]
           (validate-impl ctx (update v-ctx :path conj k) sch (get subj k)))))
      v-ctx)))

(defn validate [ctx schema resource]
  (dissoc (validate-impl ctx {:path []} schema resource) :path))

(clj-yaml.core/parse-string "

attrs:
 do[]: {}

")




