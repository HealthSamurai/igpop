(ns igpop.schema-test
  (:require [igpop.schema :as sut]
            [clojure.test :refer :all]
            [igpop.loader :as loader]
            [matcho.core :as matcho]
            [clojure.java.io :as io]
            [cheshire.core :refer :all]))

(def project-path (.getPath (io/resource "test-project")))

(def project (loader/load-project project-path))

(def task-json-schema (io/file (io/resource "test-project/TaskJsonSchema.json")))

(deftest generate-json-schema
  (testing "generate-schema"
    (let [schema (parse-string (slurp task-json-schema) true)
          ctx project]
      (matcho/match schema
                    sut/generate-json-schema (:Task (:profiles ctx))))))
