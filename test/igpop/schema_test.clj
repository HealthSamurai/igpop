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
    (:address (:properties (:Organization (:definitions (sut/generate-schema project)))))

    (get-in project [:profiles :Organization :basic :elements :address :elements :line :maxItems])

    (sut/type-defintion (:name (:properties (:Patient (:definitions (sut/generate-schema project))))))

    (spit (io/file (.getPath (io/resource "test-project")) "../test-schema.json") (generate-string (sut/generate-schema project) {:pretty true}))

    (loader/inlined-valuesets (get-in project [:profiles]))

    (sut/fhir-type-definition "HumanName" project)

    (sut/profile-to-schema :Patient :basic (get-in project [:profiles :Patient :basic]) project)

    (sut/get-concepts project (get-in project [:profiles :AllergyIntolerance :basic :elements]))
    )

  (testing "get concepts"
    (matcho/match
     ["active" "inactive" "resolved"] (sut/get-concepts project (get-in project [:profiles :AllergyIntolerance :basic :elements :clinicalStatus])))

    (matcho/match
     ["male" "female" "androgin" "unknonw"] (sut/get-concepts project (get-in project [:profiles :Patient :basic :elements :gender])))

    ))
