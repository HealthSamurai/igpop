(ns igpop.schema-test
  (:require [igpop.schema :as sut]
            [clojure.test :refer :all]
            [matcho.core :as matcho]
            [clojure.java.io :as io]
            [igpop.loader :as loader]
            [cheshire.core :refer :all]))

(deftest test-schema-gen

  (def project-path (.getPath (io/resource "test-project")))

  (def project (loader/load-project project-path))

  (comment
    (sut/generate-schema project)

    (get-in project [:profiles :Patient :basic :elements])

    (spit (io/file "/home/victor/Documents/Trash/test-schema.json") (generate-string (sut/generate-schema project) {:pretty true}))

    (sut/get-concepts project (get-in project [:profiles :AllergyIntolerance :basic :elements :clinicalStatus]))
    )

  (testing "get concepts"
    (matcho/match
     ["active" "inactive" "resolved"] (sut/get-concepts project (get-in project [:profiles :AllergyIntolerance :basic :elements :clinicalStatus])))

    (matcho/match
     ["male" "female" "androgin" "unknonw"] (sut/get-concepts project (get-in project [:profiles :Patient :basic :elements :gender])))
    ))
