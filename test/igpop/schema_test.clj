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
    (:name (:elements (:basic (:Patient (:profiles project)))))

    (:name (:properties (:Patient (:definitions (sut/generate-schema project)))))

    (keys (:definitions (sut/profile-definitions :Organization (:properties (:Organization (:definitions (sut/generate-schema project)))) project)))

    (spit (io/file (str (System/getProperty "user.dir") "/test-schema.json")) (generate-string (sut/generate-schema project) {:pretty true}))

    (sut/extract-simple-types (:properties (:Patient (:definitions (sut/generate-schema project)))) project)

    (:address (:properties (:Organization (:definitions (sut/generate-schema project)))))

    (sut/extract-type-def (:address (:properties (:Organization (:definitions (sut/generate-schema project))))) project "Organization")

    (sut/get-fhir-complex-def :Address project)

    (:properties (:Organization (sut/profile-to-schema :Organization :basic (get-in project [:profiles :Organization :basic]) project)))

    (:properties (:Organization (sut/profile-to-schema :Organization :basic (get-in project [:profiles :Organization :basic]) project)))

    (sut/shape-up-definitions :Organization :basic (:properties (:Organization (sut/profile-to-schema :Organization :basic (get-in project [:profiles :Organization :basic]) project))) project)

    (spit (io/file "/home/victor/Documents/Trash/defs.json") (generate-string (:definitions (sut/shape-up-definitions :Organization :basic (:properties (:Organization (sut/profile-to-schema :Organization :basic (get-in project [:profiles :Organization :basic]) project))) project)) {:pretty true}))

    (sut/extract-element-def (:address (:properties (:Organization (sut/profile-to-schema :Organization :basic (get-in project [:profiles :Organization :basic]) project)))) project :Organization)

    (:Organization-Address (sut/enrich-element-def (sut/extract-element-def (:address (:properties (:Organization (sut/profile-to-schema :Organization :basic (get-in project [:profiles :Organization :basic]) project)))) project :Organization) project))

    (:address (:properties (:Organization (:definitions (sut/generate-schema project)))))

    (spit (io/file (.getPath (io/resource "test-project")) "../test-schema.json") (generate-string (sut/generate-schema project) {:pretty true}))

    (loader/get-inlined-valuesets (get-in project [:profiles]))

    (sut/fhir-type-definition "HumanName" project)

    (:properties (:Organization (sut/profile-to-schema :Organization :basic (get-in project [:profiles :Organization :basic]) project)))

    (sut/extract-refs [] (first (seq {:outterProperty {:$ref "test1" :properties {:firstProperty {:$ref "test2"}
                                                                                  :secondProperty {:$ref "test3"}}}})))

    (sut/get-concepts project (get-in project [:profiles :AllergyIntolerance :basic :elements]))

    (get-in project [:definitions :complex :Address])

    (get-in project [:definitions :complex])

    (keys (into {} (sut/get-refered-def (select-keys (get-in project [:definitions :complex]) [:HumanName]) project))) 

    )

  (testing "extracting refs"
    (matcho/match
     [:test1 :test2 :test3] (sut/extract-refs [] (first (seq {:outterProperty {:$ref "#/definitions/test1" :properties {:firstProperty {:$ref "#/definitions/test2"}
                                                                                                                        :secondProperty {:$ref "#/definitions/test3"}}}})))))

  (testing "get concepts"
    (matcho/match
     ["active" "inactive" "resolved"] (sut/get-concepts project (get-in project [:profiles :AllergyIntolerance :basic :elements :clinicalStatus])))

    (matcho/match
     ["male" "female" "androgin" "unknonw"] (sut/get-concepts project (get-in project [:profiles :Patient :basic :elements :gender]))))

  (testing "element to schema transformation"
    (matcho/match
     {:firstTestElement {:description "Hello"
                         :properties {:secondTestElement
                                      {:description "world"}}}} (sut/element-to-schema {} (-> {:firstTestElement {:description "Hello" :elements {:secondTestElement {:description "world"}}}}
                                                                                              seq
                                                                                              first) project))

    (matcho/match
     {:firstTestElement {:description "Hello"
                         :properties {:secondTestElement
                                      {:description "world"
                                       :properties {:thirdTestElement
                                                    {:description "!"
                                                     :properties {:fourthTestElement
                                                                  {:description "!!"}}}}}}}}
     (sut/element-to-schema {} (-> {:firstTestElement {:description "Hello"
                                                       :elements {:secondTestElement
                                                                    {:description "world"
                                                                     :elements {:thirdTestElement
                                                                                  {:description "!"
                                                                                   :elements {:fourthTestElement
                                                                                                {:description "!!"}}}}}}}}
                                   seq
                                   first) project)) ))
