(ns igpop.loader-test
  (:require [igpop.loader :as sut]
            [clojure.test :refer :all]
            [matcho.core :as match]
            [clojure.java.io :as io]
            [matcho.core :as matcho]))

(deftest test-loader
  (testing "parse-name"

    (matcho/match
     (sut/parse-name "Patient.yaml")
     {:to [:source :Patient :basic]
      :format :yaml})

    ;; (matcho/match
    ;;  (sut/parse-name "pr.Patient.example.pt1.yaml")
    ;;  {:to [:source :Patient :basic :example :pt1]
    ;;   :format :yaml})

    (matcho/match
     (sut/parse-name "Patient" "lab.yaml")
     {:to [:source :Patient :lab]
      :format :yaml})

    (matcho/match
     (sut/parse-name "vs.dic1.yaml")
     {:to [:valuesets :dic1]
      :format :yaml})

    (matcho/match
     (sut/parse-name "vs.dic1.csv")
     {:to [:valuesets :dic1 :concepts]
      :format :csv})


    )

  (def project-path (.getPath (io/resource "test-project")))

  (def project (sut/load-project project-path))

  (io/file project-path "ig.yaml")
  (io/file project-path "node_modules" "igpop-fhir-4.0.0")

  (matcho/match
   (:base project)
   {:profiles {:Patient {:elements {:name {}}}}})

  (matcho/match
   (:source project)
   {:Patient {:basic {:elements {}}}})

  (matcho/match
   (:profiles project)
   {:Patient {:basic {:elements {:name {:type "HumanName"}}}
              :lab-report {}}})

  (get-in project [:profiles :Patient :basic])
  (get-in project [:source :Patient :basic :description])
  (get-in project [:valuesets :dict1])

  (keys project)
  (get-in project [:valuesets])

  (matcho/match
   (get-in project [:valuesets :dict1])
   {:concepts [{:code "male" :display "Male"}]})

  (is (not (nil? (get-in project [:docs :pages :welcome]))))
  (is (not (nil? (get-in project [:docs :menu]))))
  (get-in project [:docs :pages])


  )

