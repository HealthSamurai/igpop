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

  (comment (reduce (fn [acc [eln el]] (assoc-in acc [eln :desc] (get el :description))) {} (get-in project [:profiles :Task :basic :elements]))

           (sut/get-required (get-in project [:profiles :Task :basic :elements :input :elements])))

  (get-in project [:profiles :Task :basic :elements :input :elements :value])

  (testing "get concepts"
    (matcho/match
     ["male" "female"] (sut/get-concepts project :dict1))

    (matcho/match
     ["male" "female" "androgin" "unknonw"] (sut/get-concepts project :administrative-genders))

     (matcho/match
      ["draft" "active" "on-hold" "revoked" "completed" "entered-in-error" "unknown"] (sut/get-concepts project :careplan-status))))
