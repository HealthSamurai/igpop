(ns igpop.loader-test
  (:require [igpop.loader :as sut]
            [clojure.test :refer :all]
            [matcho.core :as match]
            [clojure.java.io :as io]
            [matcho.core :as matcho]))

(deftest test-loader
  (def project-path (.getPath (io/resource "test-project")))

  (def project (sut/load-project project-path))

  (io/file project-path "ig.yaml")
  (io/file project-path "node_modules" "igpop-fhir-4.0.0")

  (matcho/match
   project
   {:fhir {:profiles {:Patient {:elements {:name {}}}}}
    :profiles {:Patient {:basic {}}}}
   )

  (get-in project [:profiles :Patient :basic])

  )

