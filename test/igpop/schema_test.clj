(ns igpop.schema-test
  (:require [igpop.schema :as sut]
            [clojure.test :refer :all]
            [matcho.core :as matcho]
            [clojure.java.io :as io]
            [igpop.loader :as loader]))

(deftest test-schema-gen

  (def project-path (.getPath (io/resource "test-project")))

  (def project (loader/load-project project-path))

  (testing "get concepts"
    (matcho/match
     ["male" "female"] (sut/get-concepts project :dict1))

    (matcho/match
     ["male" "female" "androgin" "unknonw"] (sut/get-concepts project :administrative-genders))
    ))
