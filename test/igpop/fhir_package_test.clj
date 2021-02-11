(ns igpop.fhir-package-test
  (:require [clojure.test :as t :refer [deftest is testing]]
            [matcho.core :as matcho]
            [igpop.fhir-package :as fp]))


(deftest ->fhir-package-test
  (testing "->fhir-package should"
    (testing "pass correct values"
      (is
       (= {:name         "example.fhir.ig.ae"
           :version      "1.0.0"
           :canonical    "http://example.com"
           :url          "http://example.com"
           :title        "Some Sample"
           :description  "Some Sample Desc"
           :fhirVersions ["4.0.1"]
           :dependencies {"hl7.fhir.core" "4.0.1"}
           :keywords     ["fhir" "example" "test"]
           :author       "Tester"
           :maintainers  [{"name" "Tester"
                           "email" "tester@gmail.com"}]
           :license "MIT"}
          (fp/->package-json
           {:id "example.fhir.ig.ae"
            :version "1.0.0"
            :title "Some Sample"
            :url "http://example.com"
            :description "Some Sample Desc"
            :fhir "4.0.1"
            :keywords ["fhir" "example" "test"]
            :author "Tester"
            :maintainers [{"name" "Tester"
                           "email" "tester@gmail.com"}]
            :licence "MIT"}))))

    (testing "fill with empty strings missed values"
      (is
       (= {:name         ""
           :version      ""
           :canonical    ""
           :url          ""
           :title        ""
           :description  ""
           :fhirVersions [""]
           :dependencies {"hl7.fhir.core" ""}
           :keywords     []
           :author       ""
           :maintainers  []
           :license "CC0-1.0"}
          (fp/->package-json {}))))

    (testing "return map with correct key order"
      (is (= [:name :version :canonical :url :title :description :fhirVersions
              :dependencies :keywords :author :maintainers :license]
             (keys (fp/->package-json {})))))))



(deftest ->index-json-test
  (testing "->index-json should"
    (testing "return map with correct key order"
      (is (= [:index-version :files]
             (keys (fp/->index-json nil)))))

    (testing "create correct files document from resource"
      (matcho/match
       (fp/->index-json
        [{:id "AdverseEvent" :resourceType "StructureDefinition"
          :url "http://example.com/SructureDefintions/AdverseEvent"
          :kind "resource" :version "0.0.1" :type "AdverseEvent"}])
        {:files
         [{:resourceType "StructureDefinition"
           :id "AdverseEvent"
           :url "http://example.com/SructureDefintions/AdverseEvent"
           :version "0.0.1"
           :kind "resource"
           :type "AdverseEvent"}]}))

    (testing "create correct files names according to resourcetype"
      (matcho/match
       (fp/->index-json
        [{:id "AdverseEvent" :resourceType "StructureDefinition"}
         {:id "condition-outcome" :resourceType "ValueSet"}
         {:id "Condition-outcome" :resourceType "CodeSystem"}])
        {:files
         [{:filename "StructureDefinition-AdverseEvent.json"}
          {:filename "ValueSet-condition-outcome.json"}
          {:filename "CodeSystem-Condition-outcome.json"}]}))

    (testing "not create :version,:kind,:type properties if they are not exists in resource"
      (is (= [:filename :resourceType :id :url]
             (-> [{:id "AdverseEvent" :resourceType "StructureDefinition"
                   :url "http://example.com"}]
                 fp/->index-json
                 :files
                 first
                 keys))))))

