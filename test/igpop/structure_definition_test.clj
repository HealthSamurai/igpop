(ns igpop.structure-definition-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [matcho.core :as matcho]
            [igpop.loader :as loader]
            [igpop.structure-definition :as sd]))

(deftest prop->sd-test
  (let [element {}
        id "Patient.telecom.system"
        path "Patient.telecom.system"]
    (testing "default"
      (is (= {:test 0} (sd/prop->sd element id path :test 0))
          "Property should be preserved by default"))
    (testing "cardinality"
      (is (= {:min 1} (sd/prop->sd element id path :required true))
          "Required property should result in a min = 1 restriction.")
      (is (nil? (sd/prop->sd element id path :required false))
          "Non-required property should not result in a restriction.")
      (is (= {:max 0} (sd/prop->sd element id path :disabled true))
          "Disabled property should result in a max = 0 restriction.")
      (is (nil? (sd/prop->sd element id path :disabled false))
          "Non-disabled property should not result in a restriction.")
      (is (= {:min 7} (sd/prop->sd element id path :minItems 7))
          "minItems property should result in corresponding restriction.")
      (is (= {:max 42} (sd/prop->sd element id path :maxItems 42))
          "maxItems property should result in corresponding restriction."))
    (testing "constant"
      (is (= {:fixedSystem "email"} (sd/prop->sd element id path :constant "email"))
          "Constant should result in corresponding `fixedX` restriction."))
    (testing "constraints"
      (let [constraint {:ele-1 {:description "All FHIR elements"}
                        :ext-1 {:expression "Must have either" :severity "init"}}]
        (is (= {:constraint [{:key "ele-1", :severity "error", :human "All FHIR elements"}
                             {:key "ext-1", :severity "init", :expression "Must have either"}]}
               (sd/prop->sd element id path :constraints constraint)))))
    (testing "union"
      (is (= {:id "ActivityDefinition.subject[x]"
              :path "ActivityDefinition.subject[x]"
              :type [{:code "CodeableConcept"}, {:code "Reference"}]}
             (sd/prop->sd element
                          "ActivityDefinition.subject"
                          "ActivityDefinition.subject"
                          :union
                          ["CodeableConcept", "Reference"]))
          "Union property should result in corresponding restriction."))
    (testing "refers"
      (is (= {:type
              [{:code "Reference"
                :targetProfile ["https://healthsamurai.github.io/igpop/profiles/Practitioner/basic.html"]}
               {:code "Reference"
                :targetProfile ["https://healthsamurai.github.io/igpop/profiles/Organization/basic.html"]}
               {:code "Reference"
                :targetProfile ["https://healthsamurai.github.io/igpop/profiles/Patient/basic.html"]}]}
             (sd/prop->sd element id path :refers [{:profile "basic"
                                                    :resourceType "Practitioner"}
                                                   {:resourceType "Organization"
                                                    :profile "basic"}
                                                   {:profile "basic"
                                                    :resourceType "Patient"}]))
          "Reference should result in type restriction with correct URL to the referenced resource."))
    (testing "valueset"
      (is (= {:binding {:valueSet "https://healthsamurai.github.io/igpop/valuesets/sample.html"
                        :description "test"
                        :strength "extensible"}}
             (sd/prop->sd element id path :valueset {:id "sample" :description "test"}))
          "Valueset should result in binding with correct URL and description."))
    (testing "mappings"
      (is (= {:mapping [{:identity "hl7.v2" :map "PID-5, PID-9"}
                        {:identity "ru.tfoms" :map "XX-XX-F1"}]}
             (sd/prop->sd element id path :mappings {:hl7.v2 {:map "PID-5, PID-9"}, :ru.tfoms {:map "XX-XX-F1"}}))
          "Mappings should result in a list of mapping structures."))
    (testing "description"
      (is (= {:short "test"}
             (sd/prop->sd element id path :description "test"))
          "Description should go to `short` field of the structure definition."))))

(deftest path->id-test
  (is (= "Patient.telecom.system" (sd/path->id [:Patient :telecom :system]))
      "Parts of the path should be joined with a dot.")
  (is (= "Patient.extension:ethnicity" (sd/path->id [:Patient :Extension :ethnicity]))
      "Extension id should be joined with a colon."))

(deftest path->str-test
  (is (= "Patient.telecom.system" (sd/path->str [:Patient :telecom :system]))
      "Parts of the path should be joined with a dot.")
  (is (= "Patient.extension" (sd/path->str [:Patient :Extension :ethnicity]))
      "Extension id should not apear in the path."))

(deftest element->sd-test
  (is (= {:id "Patient.identifier"
          :path "Patient.identifier"
          :mustSupport true
          :min 1
          :max 1
          :short "Identifier"}
         (sd/element->sd [:Patient :identifier]
                         {:minItems 1
                          :maxItems 1
                          :description "Identifier"})))
  (is (= {:id "Patient.gender"
          :path "Patient.gender"
          :mustSupport true
          :binding {:valueSet "https://healthsamurai.github.io/igpop/valuesets/fhir:administrative-gender.html"
                    :strength "extensible"
                    :description nil}}
         (sd/element->sd [:Patient :gender]
                         {:valueset {:id "fhir:administrative-gender"}}))))

(def patient
  {:description "Patient profile"
   :elements
   {:Extension {:race {:description "US Core Race Extension"
                       :elements {:text {:required true,  :description "Race Text", :type "string"}
                                  :ombCategory {:collection true :type "Coding" :valueset {:id "detailed-race"}}}}
                :birthsex {:type "code", :valueset {:id "birthsex"}}}
    :identifier {:min 1
                 :elements {:system {:required true}
                            :value {:required true
                                    :description "Description"}}}}})

(deftest flatten-element-test
  (is (= {[:Patient :identifier] {:min 1}
          [:Patient :identifier :system] {:required true}
          [:Patient :identifier :value] {:required true, :description "Description"}}
         (sd/flatten-element [:Patient :identifier]
                             (get-in patient [:elements :identifier]))))
  (is (= [[:Patient :Extension :race]
          [:Patient :Extension :birthsex]]
         (keys
          (sd/flatten-element [:Patient :Extension]
                              (get-in patient [:elements :Extension]))))))

(deftest convert-test
  (matcho/match
   (sd/convert :Patient patient)
    [{:id "Patient"
      :path "Patient"
      :mustSupport true
      :short "Patient profile"}
     {:id "Patient.extension:race"
      :path "Patient.extension"
      :mustSupport true
      :short "US Core Race Extension"}
     {:id "Patient.extension:birthsex"
      :path "Patient.extension"
      :mustSupport true
      :binding {}}
     {:id "Patient.identifier"
      :path "Patient.identifier"
      :mustSupport true
      :min 1}
     {:id "Patient.identifier.system"
      :path "Patient.identifier.system"
      :mustSupport true
      :min 1}
     {:id "Patient.identifier.value"
      :path "Patient.identifier.value"
      :mustSupport true
      :min 1
      :short "Description"}]))

(deftest profile->structure-definition-test
  (let [result (sd/profile->structure-definition :Patient :basic patient patient)]
    (is (= "StructureDefinition" (:resourceType result)))
    (is (= "basic" (:id result)))
    (is (= "Patient" (:type result)))
    (is (contains? (:differential result) :element))
    (is (= 6 (-> (:differential result) :element count)))))

(deftest project->bundle-test
  (let [project-path (.getPath (io/resource "test-project"))
        project (loader/load-project project-path)
        result (sd/project->bundle project)]
    (is (= "Bundle" (:resourceType result)))
    (is (= "resources" (:id result)))
    (is (= "collection" (:type result)))
    (is (contains? result :meta))
    (is (not-empty (:entry result)))
    (is (->> (:entry result)
             (every? #(and (contains? % :fullUrl)
                           (contains? % :resource)))))))

(deftest generate-package!-test
  (let [project-path (.getPath (io/resource "test-project"))
        project (loader/load-project project-path)
        package (sd/generate-package! :npm project)]
    (is (.exists package) "Generated package should exist.")
    ;; TODO validate content of the package
    (io/delete-file package)))
