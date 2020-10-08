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

      (is (= {:max "0"} (sd/prop->sd element id path :disabled true))
          "Disabled property should result in a max = 0 restriction.")

      (is (nil? (sd/prop->sd element id path :disabled false))
          "Non-disabled property should not result in a restriction.")

      (is (= {:min 7} (sd/prop->sd element id path :minItems 7))
          "minItems property should result in corresponding restriction.")

      (is (= {:max "42"} (sd/prop->sd element id path :maxItems 42))
          "maxItems property should result in corresponding restriction.")

      (is (= {:min 0 :max "*"} (sd/prop->sd element id path :collection true))
          "collection property should result in both - minItems & maxItems restrictions")
      )

    (testing "type"
      (is (= {:type [{:code "string"}]} (sd/prop->sd element id path :type "string"))
          "'type' property with simple value should be wrapped in [{:code <type>}]")

      (is (= {:type [{}]} (sd/prop->sd element id path :type [{}]))
          "'type' property with complex value(vector of maps)- should not be changed")

      (is (= {:type :ANYTHING} (sd/prop->sd (merge element {:union []}) id path :type :ANYTHING))
          "'type' property of element with union type should not be changed"))

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
  (is (= "Patient.extension:ethnicity" (sd/path->id [:Patient :extension :ethnicity]))
      "Extension id should be joined with a colon."))

(deftest path->str-test
  (is (= "Patient.telecom.system" (sd/path->str [:Patient :telecom :system]))
      "Parts of the path should be joined with a dot.")
  (is (= "Patient.extension" (sd/path->str [:Patient :extension :ethnicity]))
      "Extension id should not apear in the path."))

(deftest element->sd-test
  (is (= {:id "Patient.identifier"
          :path "Patient.identifier"
          :mustSupport true
          :min 1
          :max "1"
          :short "Identifier"}
         (sd/element->sd [[:Patient :identifier]
                          {:minItems 1
                           :maxItems 1
                           :description "Identifier"}])))
  (is (= {:id "Patient.gender"
          :path "Patient.gender"
          :mustSupport true
          :binding {:valueSet "https://healthsamurai.github.io/igpop/valuesets/fhir:administrative-gender.html"
                    :strength "extensible"
                    :description nil}}
         (sd/element->sd [[:Patient :gender]
                          {:valueset {:id "fhir:administrative-gender"}}]))))

(def patient
  {:description "Patient profile"
   :elements
   {:extension {:race {:description "US Core Race Extension"
                       :url "exn:extension:patient-race"
                       :elements {:extension
                                  {:text {:required true, :description "Race Text", :type "string"}
                                   :ombCategory {:collection true, :type "Coding", :valueset {:id "omb-race-category"}}
                                   :detailed {:collection true, :type "Coding", :valueset {:id "detailed-race"}, :description "Extended race codes"}}
                                  :url {:type "uri", :constant "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race"}
                                  :value {:maxItems "0"}}}
                :birthsex {:type "code", :valueset {:id "birthsex"}}}
    :identifier {:min 1
                 :elements {:system {:required true}
                            :value {:required true
                                    :description "Description"}}}
    :address {:elements {:extension {:region {:type "CodeableConcept"}}}}}})


(deftest flatten-element-test

  (testing "flatten default elements"
    (is (= {[:Patient :identifier] {:min 1}
            [:Patient :identifier :system] {:required true}
            [:Patient :identifier :value] {:required true, :description "Description"}}
           (sd/flatten-element [:Patient :identifier]
                               (get-in patient [:elements :identifier])))))

  (testing "flatten extension elemement"
    (let [flattened (sd/flatten-element [:Patient :extension] (get-in patient [:elements :extension]))]

      (is (= (get flattened [:Patient :extension])
             {:slicing {:discriminator [{:type "value" :path "url"}]
                        :ordered false
                        :rules "open"}})
          "Should add additional element for extension description")

      (is (= [[:Patient :extension]
              [:Patient :extension :race]
              [:Patient :extension :birthsex]]
             (keys flattened))
          "Should add all extension elements."))))


(deftest convert-test
  (matcho/match
   (sd/convert :Patient patient)
   [#_{:id "Patient"
       :path "Patient"
       :mustSupport true
       :short "Patient profile"}
    {:id "Patient.extension"
     :path "Patient.extension"
     :slicing {:discriminator [{:type "value" :path "url"}]
               :ordered false
               :rules "open"}}
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

(def race-extension (get (sd/get-extensions patient) [:elements :extension :race]))


(deftest extension->structure-definition-test
  (let [race-extension (get (sd/get-extensions patient) [:elements :extension :race])
        result (sd/extension->structure-definition "hl7.fhir.test" "Patient.extension" :race race-extension race-extension)]

    (testing "Root elements should be correct"
      (matcho/match
       result
       {:resourceType   "StructureDefinition"
        :id             "hl7.fhir.test-Patient.extension-race"
        :name           "race"
        :description    "US Core Race Extension",
        :status         "active"
        :fhirVersion    "4.0.1"
        :kind           "complex-type"
        :abstract       false
        :url             "exn:extension:patient-race"
        :type           "Extension"
        :baseDefinition "http://hl7.org/fhir/StructureDefinition/Extension"
        :derivation     "constraint"
        :context        [{:type "element", :expression "Patient.extension"}],
        :differential   {:element []}}))

    #_(testing "Differential elements should be correct"
      (matcho/match
       result
       {:differential
        {:element
         [{:id "Extension", :path "Extension", :mustSupport true, :short "US Core Race Extension"}
          {:id "Extension.extension", :path "Extension.extension", :mustSupport true,
           :slicing {:discriminator [{:type "value", :path "url"}], :ordered false, :rules "open"}}
          {:id "Extension.extension:text", :path "Extension.extension", :mustSupport true, :min 1, :short "Race Text",
           :type [{:code "Extension", :profile ["https://healthsamurai.github.io/ig-ae/profiles/Extension/text"]}],
           :sliceName "text", :isModifier false}
          {:id "Extension.extension:ombCategory", :path "Extension.extension", :mustSupport true, :min 0 :max "*"
           :type [{:code "Extension", :profile ["https://healthsamurai.github.io/ig-ae/profiles/Extension/ombCategory"]}],
           :binding {:valueSet "https://healthsamurai.github.io/igpop/valuesets/omb-race-category.html",
                     :strength "extensible", :description nil}, :sliceName "ombCategory", :isModifier false}
          {:id "Extension.extension:detailed", :path "Extension.extension", :mustSupport true, :min 0 :max "*"
           :type [{:code "Extension", :profile ["https://healthsamurai.github.io/ig-ae/profiles/Extension/detailed"]}],
           :binding {:valueSet "https://healthsamurai.github.io/igpop/valuesets/detailed-race.html", :strength "extensible", :description nil},
           :short "Extended race codes", :sliceName "detailed", :isModifier false}
          {:id "Extension.url", :path "Extension.url", :mustSupport true, :type [{:code "uri"}], :fixedUrl "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race"}
          {:id "Extension.value", :path "Extension.value", :mustSupport true, :max "0" :type [{}]}]}}))

    (testing "When simple (non-nested) extension -"

      (let [ext {:type "string", :description "AZ Employee Reporter" :url "http:///hl7.fhir.test/AZAdverseEvent/AZEmployeeReporter"
                 :minItems 2, :maxItems 4}
            res (sd/extension->structure-definition "hl7.fhir.test" "AZAdverseEvent" :AZEmployeeReporter ext ext)]

        (testing " additional elements should be generated - 'extension', 'url' and 'value[x]'"
          (matcho/match
           res
           {:differential
            {:element
             [{:id "Extension" :path "Extension"}
              {:id "Extension.extension" :path "Extension.extension"}
              {:id "Extension.url" :path  "Extension.url"}
              {:id "Extension.value[x]" :path "Extension.value[x]" :type [{:code "string"}]}]}}))

        (testing " minItems and maxItems property should go to 'Extension' element. Rest elements should have constant values"
          (matcho/match
           res
           {:differential {:element
                           [{:id "Extension"           :min 2 :max "4"}
                            {:id "Extension.extension" :min 0 :max "0"}
                            {:id "Extension.url"       :min 1 :max "1"}
                            {:id "Extension.value[x]"  :min 1 :max "1"}]}}))

        (testing "'url' field of profile should go to 'fixedUri' property of 'Element.url'"
          (matcho/match
           res {:differential {:element [{} {} {:id "Extension.url" :fixedUri "http:///hl7.fhir.test/AZAdverseEvent/AZEmployeeReporter"} {}]}}))))


    (testing "When 'url' prop is given"
      (let [ext {:url "urn:extension:sometype-someextension" }
            res (sd/extension->structure-definition "project-id" "SomeType" :SomeExtension ext ext)]

        (testing "it should be passed to top-level property of StructureDefinition - 'url'"
          (matcho/match res {:url "urn:extension:sometype-someextension"}))

        (testing "it should not be passed to differential elements"
          (is (= [nil nil nil nil] (map :url (get-in res [:differential :element]))))))
      )))

(deftest profile->structure-definition-test
  (let [result (sd/profile->structure-definition "hl7.fhir.test" :Patient :basic patient patient)]
    (matcho/match
     result
     {:resourceType "StructureDefinition"
      :id "hl7.fhir.test-Patient"
      :type "Patient"
      :differential {:element [#_{:id "Patient"}
                               {:id "Patient.extension"}
                               {:id "Patient.extension:race" :type [{:code "Extension"}]}
                               {:id "Patient.extension:birthsex" :type [{:code "Extension"}]}
                               {:id "Patient.identifier"}]}})))

(deftest get-extensions-test
  (matcho/match
   (sd/get-extensions patient)
   {[:elements :extension :race] {}
    [:elements :extension :race :elements :extension :text] {}
    [:elements :extension :birthsex] {}
    [:elements :address :elements :extension :region] {}}))


(sd/get-extensions patient)

(deftest ig-profile->structure-definitions
  (let [result (sd/ig-profile->structure-definitions "hl7.fhir.test" :Patient :basic patient patient)]
    (matcho/match
     result
     [{:resourceType "StructureDefinition", :id "hl7.fhir.test-Patient" :type "Patient"}
      {:resourceType "StructureDefinition", :id "hl7.fhir.test-Patient-region", :type "Extension"}
      {:resourceType "StructureDefinition", :id "hl7.fhir.test-Patient-race", :type "Extension"}
      {:resourceType "StructureDefinition", :id "hl7.fhir.test-Patient-text", :type "Extension"}
      {:resourceType "StructureDefinition", :id "hl7.fhir.test-Patient-ombCategory", :type "Extension"}
      {:resourceType "StructureDefinition", :id "hl7.fhir.test-Patient-detailed", :type "Extension"}
      {:resourceType "StructureDefinition", :id "hl7.fhir.test-Patient-birthsex", :type "Extension"}
      ])))

(deftest ig-vs->valueset-test
  (let [valueset (first 
                  {:survey-status
                   {:system "Intellijent source"
                    :concepts
                    [{:code "req-det", :display "Requested detailed investigation"}
                     {:code "N/A", :display "No further cooperation is available"}]}})]
    (matcho/match
     (sd/ig-vs->valueset "hl7.fhir.test" valueset)
     {:resourceType "ValueSet"
      :id "hl7.fhir.test-survey-status"
      :name "SurveyStatus"
      :title "survey status"
      :status "active"
      :compose {:include [{:system "Intellijent source"
                           :concept (:concepts (val valueset))}]}})))

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
    #_(io/delete-file package)))


