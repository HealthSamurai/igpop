(ns igpop.structure-def-test
  (:require [igpop.structure-def :as sdef]
            [clojure.test :refer :all]
            [matcho.core :as matcho]
            [clojure.java.io :as io]
            [igpop.loader :as loader]
            [cheshire.core :refer :all]))

(deftest test-structure-gen

  (def profile
    {:Patient
     {:elements
      {:name {:type "HumanName"
              :minItem 1
              :elements
              {:family {:type "string" :isCollection true :minItem 1 :maxItem 10}}}}}})

  (comment (testing "collection cardinality"
             (matcho/match
              (sdef/to-sd profile)
              {:snapshot [{:path "Patient.name" :min 1}]})))

  (def plain-elements-in
    {:CarePlan.subject {}
     :CarePlan.text {}})

  (testing "elements-to-sd func"
    (matcho/match
     (sdef/elements-to-sd plain-elements-in)
                          [{:id "CarePlan.subject",
                            :path "CarePlan.subject"},
                           {:id "CarePlan.text",
                            :path "CarePlan.text"}]))

  (testing "cardinality required"
    (matcho/match
     (sdef/cardinality :required true)
     {:min 1}))

  (testing "cardinality disabled"
    (matcho/match
     (sdef/cardinality :disabled true)
     {:max 0}))

  (testing "cardinality minItems"
    (matcho/match
     (sdef/cardinality :minItems 5)
     {:min 5}))

  (testing "cardinality maxItems"
    (matcho/match
     (sdef/cardinality :maxItems 14)
     {:max 14}))

  (testing "mustSupport default"
    (matcho/match
     (sdef/mustSupport)
     {:mustSupport true}))

  (testing "mustSupport from profile"
    (matcho/match
     (sdef/mustSupport false)
     {:mustSupport false}))

  (def project-path (.getPath (io/resource "test-project")))

  (def project (loader/load-project project-path))

  (comment

    (println project)

    (spit (io/file (str (System/getProperty "user.dir") "/show-project.json")) (generate-string project {:pretty true}))

    (println (sdef/generate-structure project))

    (get-in project [:profiles :Patient :basic :elements :name :elements :given])

    (spit (io/file (str (System/getProperty "user.dir") "/test-structure-def.json")) (generate-string (sdef/generate-structure project) {:pretty true}))

    ))

