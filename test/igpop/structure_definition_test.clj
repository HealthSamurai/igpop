(ns igpop.structure-definition-test
  (:require [clojure.test :refer [deftest is testing are]]
            [clojure.java.io :as io]
            [matcho.core :as matcho]
            [igpop.loader :as loader]
            [igpop.structure-definition :as sd]
            [clojure.string :as str]))

(deftest resource-root-keys-test
  (is (= sd/resource-root-keys
         [:resourceType :id :url :identifier :version :name :title :status
          :experimental :date :publisher :contact :description :useContext :jurisdiction
          :purpose :copyright :keyword :fhirVersion :mapping :kind :abstract :context
          :contextInvariant :type :baseDefinition :derivation :snapshot :differential])
      (str "Keys should be correct and placed in correct order. "
           "According to https://www.hl7.org/fhir/structuredefinition.html")))

(deftest url?-test
  (are [x y] (= (sd/url? x) y)
    "http" true
    "htt"  false
    ""     false
    10     false))

(deftest prop->sd-test
  (let [element {}
        id "Patient.telecom.system"
        path "Patient.telecom.system"]

    (testing "default"
      (is (= {:test 0} (sd/prop->sd nil element id path :test 0))
          "Property should be preserved by default"))

    (testing "cardinality"
      (is (= {:min 1} (sd/prop->sd nil element id path :required true))
          "Required property should result in a min = 1 restriction.")

      (is (nil? (sd/prop->sd nil element id path :required false))
          "Non-required property should not result in a restriction.")

      (is (= {:max "0"} (sd/prop->sd nil element id path :disabled true))
          "Disabled property should result in a max = 0 restriction.")

      (is (nil? (sd/prop->sd nil element id path :disabled false))
          "Non-disabled property should not result in a restriction.")

      (is (= {:min 7} (sd/prop->sd nil element id path :minItems 7))
          "minItems property should result in corresponding restriction.")

      (is (= {:max "42"} (sd/prop->sd nil element id path :maxItems 42))
          "maxItems property should result in corresponding restriction.")

      (is (= {:min 0 :max "*"} (sd/prop->sd nil element id path :collection true))
          "collection property should result in both - minItems & maxItems restrictions")
      )

    (testing "type"
      (is (= {:type [{:code "string"}]} (sd/prop->sd nil element id path :type "string"))
          "'type' property with simple value should be wrapped in [{:code <type>}]")

      (is (= {:type [{}]} (sd/prop->sd nil element id path :type [{}]))
          "'type' property with complex value(vector of maps)- should not be changed")

      (is (= {:type :ANYTHING} (sd/prop->sd nil (merge element {:union []}) id path :type :ANYTHING))
          "'type' property of element with union type should not be changed"))

    (testing "constant"
      (is (= {:fixedSystem "email"} (sd/prop->sd nil element id path :constant "email"))
          "Constant should result in corresponding `fixedX` restriction."))

    (testing "constraints"
      (let [constraint {:ele-1 {:description "All FHIR elements"}
                        :ext-1 {:expression "Must have either" :severity "init"}}]
        (is (= {:constraint [{:key "ele-1", :severity "error", :human "All FHIR elements"}
                             {:key "ext-1", :severity "init", :expression "Must have either"}]}
               (sd/prop->sd nil element id path :constraints constraint)))))

    (testing "union"
      (is (= {:id "ActivityDefinition.subject[x]"
              :path "ActivityDefinition.subject[x]"
              :type [{:code "CodeableConcept"}, {:code "Reference"}]}
             (sd/prop->sd nil element
                          "ActivityDefinition.subject"
                          "ActivityDefinition.subject"
                          :union
                          ["CodeableConcept", "Reference"]))
          "Union property should result in corresponding restriction."))

    (testing "profile"
      (is (= {:type [{:profile ["http://example.com/az-HumanName"] :code "HumanName"}]}
             (sd/prop->sd nil element id path :profile "http://example.com/az-HumanName"))
          (str "value should be injected into path [:type 0 :profile]"
               " and extracted type name from url and placed by path [:type 0 :code]"))

      (is (= {:type [{:profile ["http://example.com/StructureDefinition/test.ag-HumanName"] :code "HumanName"}]}
             (sd/prop->sd {:diff-profiles {:HumanName {:basic {:baseDefinition "HumanName"}}} :id "test.ag" :url "http://example.com"}
                          element id path :profile "HumanName"))
          (str "value should be injected into path [:type 0 :profile]"
               " and retrived type-name from context by path [:diff-profiles resource-type :basic :baseDefinition] and placed by path [:type 0 :code]")) )

    (testing "refers"
      (is (= {:type
              [{:code "Reference"
                :targetProfile ["http://example.com/StructureDefinition/ig-ae-Practitioner"]}
               {:code "Reference"
                :targetProfile ["http://example.com/StructureDefinition/ig-ae-Organization"]}
               {:code "Reference"
                :targetProfile ["http://example.com/StructureDefinition/ig-ae-Patient"]}]}
             (sd/prop->sd {:url "http://example.com", :id "ig-ae"}
                          element id path :refers
                          [{:profile "basic"
                            :resourceType "Practitioner"}
                           {:resourceType "Organization"
                            :profile "basic"}
                           {:profile "basic"
                            :resourceType "Patient"}]))
          "Reference should result in type restriction with correct URL to the referenced resource."))
    (testing "valueset"
      (is (= {:binding {:valueSet "http://example.com/ValueSet/ig-sample"
                        :description "test"
                        :strength "extensible"}}
             (sd/prop->sd {:url "http://example.com" :id "ig"} element id path :valueset {:id "sample" :description "test"}))
          "Valueset should result in binding with correct URL and description."))

    (testing "mappings"
      (is (= {:mapping [{:identity "hl7.v2" :map "PID-5, PID-9"}
                        {:identity "ru.tfoms" :map "XX-XX-F1"}]}
             (sd/prop->sd nil element id path :mappings {:hl7.v2 {:map "PID-5, PID-9"}, :ru.tfoms {:map "XX-XX-F1"}}))
          "Mappings should result in a list of mapping structures."))

    (testing "description"
      (is (= {:short "test"}
             (sd/prop->sd nil element id path :description "test"))
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
         (sd/element->sd {:url "https://healthsamurai.github.io/igpop" :id "igpop"}
                         [[:Patient :identifier]
                          {:minItems 1
                           :maxItems 1
                           :description "Identifier"}])))

  (is (= {:id "Patient.gender"
          :path "Patient.gender"
          :mustSupport true
          :binding {:valueSet "https://healthsamurai.github.io/igpop/ValueSet/igpop-administrative-gender"
                    :strength "extensible"
                    :description nil}}
         (sd/element->sd {:url "https://healthsamurai.github.io/igpop" :id "igpop"}
                         [[:Patient :gender]
                          {:valueset {:id "administrative-gender"}}]))))


;; patient profile without nested extensions
(def patient-basic
  {:description "Patient profile"
   :title "Basic patient profile"
   :elements
   {:extension {:race {:description "US Core Race Extension"
                       :title "US Core Race Extension. "
                       :url "exn:extension:patient-race"
                       :elements {:url {:type "uri", :constant "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race"}
                                  :value {:maxItems "0"}}}
                :birthsex {:type "code", :valueset {:id "birthsex"}}}
    :identifier {:min 1
                 :elements {:system {:required true}
                            :value {:required true
                                    :description "Description"}}}
    :address {:elements {:extension {:region {:type "CodeableConcept"}}}}}})


(def patient
  {:description "Patient profile"
   :title "Basic patient profile"
   :elements
   {:extension {:race {:description "US Core Race Extension"
                       :title "US Core Race Extension. "
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
           (sd/flatten-element {} [:Patient :identifier]
                               (get-in patient [:elements :identifier])))))

  (testing "flatten extension elemement"
    (let [flattened (sd/flatten-element
                     {} [:Patient :extension] (get-in patient [:elements :extension]))]

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


(deftest convert-profile-elements-test
  (matcho/match
   (sd/convert-profile-elements {} :Patient patient)
   [{:id "Patient.extension"}
    {:id "Patient.extension:race"}
    ;; NOTE: Disabled nested extensions and nested extension elements for now (Vitaly 18.10.2020)
    ;; {:id "Patient.extension:race.extension"}
    ;; {:id "Patient.extension:race.extension:text"}
    ;; {:id "Patient.extension:race.extension:ombCategory"}
    ;; {:id "Patient.extension:race.extension:detailed"}
    ;; {:id "Patient.extension:race.url"}
    ;; {:id "Patient.extension:race.value"}
    ;; NOTE: Disabled nested extensions and nested extension elements for now (Vitaly 18.10.2020)
    {:id "Patient.extension:birthsex"}
    {:id "Patient.identifier"}
    {:id "Patient.identifier.system"}
    {:id "Patient.identifier.value"}
    {:id "Patient.address"}
    {:id "Patient.address.extension"}
    {:id "Patient.address.extension:region"}]))

(def race-extension (get (sd/get-extensions patient) [:elements :extension :race]))


(deftest extension->structure-definition-test
  (let [race-extension (get (sd/get-extensions patient) [:elements :extension :race])
        manifest {:id "hl7.fhir.test" :fhir "4.0.1", :url "http://example.com"}
        result (sd/extension->structure-definition manifest "Patient.extension" :race race-extension race-extension)]

    (testing "Root static properties should be correct"
      (matcho/match
       result
       {:resourceType   "StructureDefinition"
        :status         "active"
        :kind           "complex-type"
        :abstract       false
        :type           "Extension"
        :baseDefinition "http://hl7.org/fhir/StructureDefinition/Extension"
        :derivation     "constraint"}))

    (testing "Root evaluated properties should be correct"
      (matcho/match
       result
       {:id             "hl7.fhir.test-Patient.extension-race"
        :name           "race"
        :description    "US Core Race Extension",
        :context        [{:type "element", :expression "Patient.extension"}],
        :differential   {:element []}}))


    (testing "Root additional properties should be correct"
      (matcho/match
       result
       {:title "US Core Race Extension. "}))


    (testing "fhirVersion should be taken from manifest"
      (matcho/match result {:fhirVersion "4.0.1"}))

    (testing "url prefix should be taken from manifest"
      (is (str/starts-with? (:url result) (:url manifest))))

    (testing "url postfix should be taken generated resouce id"
      (is (str/ends-with? (:url result) (:id result))))



    #_(testing "Differential elements should be correct"
      (matcho/match
       result
       {:differential
        {:element
         [{:id "Extension", :path "Extension", :mustSupport true, :short "US Core Race Extension"}
          {:id "Extension.extension", :path "Extension.extension", :mustSupport true,
           :slicing {:discriminator [{:type "value", :path "url"}], :ordered false, :rules "open"}}
          {:id "Extension.extension:text", :path "Extension.extension", :mustSupport true, :min 1, :short "Race Text",
           :type [{:code "Extension", :profile ["https://healthsamurai.github.io/ig-ae/StructureDefinition/Extension/text"]}],
           :sliceName "text", :isModifier false}
          {:id "Extension.extension:ombCategory", :path "Extension.extension", :mustSupport true, :min 0 :max "*"
           :type [{:code "Extension", :profile ["https://healthsamurai.github.io/ig-ae/StructureDefinition/Extension/ombCategory"]}],
           :binding {:valueSet "https://healthsamurai.github.io/igpop/valuesets/omb-race-category",
                     :strength "extensible", :description nil}, :sliceName "ombCategory", :isModifier false}
          {:id "Extension.extension:detailed", :path "Extension.extension", :mustSupport true, :min 0 :max "*"
           :type [{:code "Extension", :profile ["https://healthsamurai.github.io/ig-ae/StructureDefinition/Extension/detailed"]}],
           :binding {:valueSet "https://healthsamurai.github.io/igpop/valuesets/detailed-race", :strength "extensible", :description nil},
           :short "Extended race codes", :sliceName "detailed", :isModifier false}
          {:id "Extension.url", :path "Extension.url", :mustSupport true, :type [{:code "uri"}], :fixedUrl "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race"}
          {:id "Extension.value", :path "Extension.value", :mustSupport true, :max "0" :type [{}]}]}}))

    (testing "When simple (non-nested) extension -"

      (let [ext {:type "string", :description "AZ Employee Reporter"
                 :minItems 2, :maxItems 4}
            manifest {:id "hl7.fhir.test" :url "http://example.com"}
            res (sd/extension->structure-definition manifest "AZAdverseEvent" :AZEmployeeReporter ext ext)]

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

        (testing "generated 'url' of sd should go to 'fixedUri' property of 'Element.url'"
          (matcho/match
           res {:differential {:element [{} {} {:id "Extension.url" :fixedUri "http://example.com/StructureDefinition/hl7.fhir.test-AZAdverseEvent-AZEmployeeReporter"} {}]}}))))


    ;; TODO: Dissalow to provide url property completely
    (testing "When 'url' prop is given"
      (let [ext {:url "urn:extension:sometype-someextension" }
            manifest {:id "project-id" :url "http://example.com"}
            res (sd/extension->structure-definition manifest "SomeType" :SomeExtension ext ext)]

        (testing "it should not be passed as postfix to top-level property of StructureDefinition - 'url'"
          (is (not (str/ends-with? (:url res) (:url ext)))))

        ;; (testing "it should be passed as postfix to top-level property of StructureDefinition - 'url'"
        ;;   (is (str/ends-with? (:url res) (:url ext))))

        (testing "it should not be passed to differential elements"
          (is (= [nil nil nil nil] (map :url (get-in res [:differential :element]))))))
      )))

(deftest profile->structure-definition-test
  (testing "converting profile to structure-definition"
    (let [result (sd/profile->structure-definition {:id "hl7.fhir.test"
                                                    :url "http://example.com"} :Patient :basic patient-basic patient-basic)]

      (testing "should generate correct root properties"
        (matcho/match result {:resourceType "StructureDefinition"
                              :id "hl7.fhir.test-Patient"
                              :type "Patient"
                              :url "http://example.com/StructureDefinition/hl7.fhir.test-Patient"
                              :differential {}}))

      (testing "should generate correct 'additional' root properties"
        (matcho/match result {:title "Basic patient profile"}))

      (testing "should generate elements in differential with correct ids and types"
        (def *res result)
        (matcho/match
         result {:differential {:element [{:id "Patient.extension"}
                                          {:id "Patient.extension:race" :type [{:code "Extension"}]}
                                          {:id "Patient.extension:birthsex" :type [{:code "Extension"}]}
                                          {:id "Patient.identifier"}]}}))

      #_(testing "should generate elements in differential with correct ids and types"
          (matcho/match
           result {:differential {:element [{:id "Patient.extension"}
                                            {:id "Patient.extension:race" :type [{:code "Extension"}]}
                                            {:id "Patient.extension:birthsex" :type [{:code "Extension"}]}
                                            {:id "Patient.identifier"}]}}))
      )))

(deftest get-extensions-test
  (matcho/match
   (sd/get-extensions patient)
   {[:elements :extension :race] {}
    [:elements :extension :race :elements :extension :text] {}
    [:elements :extension :birthsex] {}
    [:elements :address :elements :extension :region] {}}))



(deftest ig-profile->structure-definitions
  (let [manifest {:id "hl7.fhir.test" :url "http://example.com"}
        result (sd/ig-profile->structure-definitions manifest :Patient :basic patient patient)]
    (matcho/match
     result
     [{:resourceType "StructureDefinition", :id "hl7.fhir.test-Patient" :type "Patient"}
      {:resourceType "StructureDefinition", :id "hl7.fhir.test-Patient-race", :type "Extension"}
      {:resourceType "StructureDefinition", :id "hl7.fhir.test-Patient-text", :type "Extension"}
      {:resourceType "StructureDefinition", :id "hl7.fhir.test-Patient-ombCategory", :type "Extension"}
      {:resourceType "StructureDefinition", :id "hl7.fhir.test-Patient-detailed", :type "Extension"}
      {:resourceType "StructureDefinition", :id "hl7.fhir.test-Patient-birthsex", :type "Extension"}
      {:resourceType "StructureDefinition", :id "hl7.fhir.test-Patient-region", :type "Extension"}])

    (testing "Extension elements in resource definition should have correct type.code and type.profile url in it"
      (matcho/match
       result
       [{:type "Patient"
         :differential
         {:element
          [{}
           {:id "Patient.extension:race"
            :type [{:code "Extension" :profile ["http://example.com/StructureDefinition/hl7.fhir.test-Patient-race"]}]}]}}])))

  (testing "When some of elements refers to extension profile from separare igpop-profile"
    (let [manifest {:id "hl7.fhir.test" :url "http://example.com"
                    :diff-profiles {:HumanName {:basic {:baseDefinition "HumanName"}}}}
          profile {:elements {:name {:profile "HumanName"}}}
          result (sd/ig-profile->structure-definitions manifest :Patient :basic profile profile)]
      (matcho/match
       result
       [{:differential
         {:element
          [{:id "Patient.name",
            :type [{:code "HumanName", :profile ["http://example.com/StructureDefinition/hl7.fhir.test-HumanName"]}]}]}}]))))



(deftest ig-vs->valueset-test
  (let [valueset (first
                  {:survey-status
                   {:system "Intellijent source"
                    :concepts
                    [{:code "req-det", :display "Requested detailed investigation"}
                     {:code "N/A", :display "No further cooperation is available"}]}})]
    (matcho/match
     (sd/ig-vs->valueset {:id "hl7.fhir.test"} valueset)
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



