(ns igpop.profile.profile-test
  (:require [igpop.profile.translate-profile :as prof]
            [igpop.profile.parse :as parse]
            [igpop.profile.to-json-rule :as json-rule]
            [clojure.pprint :as p]
            [clojure.test :refer :all]
            [matcho.core :as matcho]
            [clojure.java.io :as io]
            [igpop.loader :as loader]
            [cheshire.core :refer :all]
            [flatland.ordered.map :refer :all]))

(deftest test-profiles

  (def profiles {:Patient {:profile-id {:elements    {:identifier {:a 1 :b 2}
                                                      :extension  {:ex-1 {:elements {:e11 {:type "some"}}}}}
                                        :description "some description"}}})

  (testing "test extractet 3 resources to parse"
    (matcho/match
      (count (parse/denormalize-document :Patient :profile-id profiles))
      2))

  (testing "test extract resources to parse"
    (matcho/match
      (parse/denormalize-document :Patient :profile-id profiles)
      [{:elements     {:identifier     {:a 1, :b 2},
                       :extension:ex-1 {:sliceName "ex-1", :extension-reference "ex-1"}},
        :profile-type :Patient,
        :type         :DomainResource,
        :id           "profile-id"}
       {:elements     {:e11           {:sliceName :e11, :definition nil, :type "Extension"}
                       :e11.url       {:minItems 1, :maxItems "1", :type "uri", :fixedUri :e11}
                       :e11.valueSome {:minItems 1, :maxItems "1", :type "some"}
                       nil            {:definition nil}
                       :url           {:minItems 1, :maxItems "1", :type "uri", :fixedUri :e11}
                       "value[x]"     {:minItems 0, :maxItems "0"}},
        :profile-type :Extension, :type :Extension, :id "ex-1"}]))

  (def elements {:extension  {:extension1 {:description "Some extension 1"
                                           :elements    {:e1 {:required    true
                                                              :description "Some extension 1 text"
                                                              :type        "string"}
                                                         :e2
                                                             {:collection true
                                                              :minItems   1
                                                              :type       "Type"
                                                              :valueset   {:id "some-ext"}}}}}
                 :identifier {:minItems 1
                              :elements
                                        {:system {:required true}
                                         :value  {:required    true
                                                  :description "The value that is unique within the system"}}}
                 :name       {:minItems    1
                              :description "A name associated with the patient"
                              :rule        {:r1 "family.count() + given.count() >= 1"}
                              :elements    {:family {} :given {}}}})

  (testing "test translating domain resource to structure definition"
    (matcho/match
      (parse/->intermediate-representation elements [:Patient])
      {[:Patient :extension]          {:extension1 {:description "Some extension 1",
                                                    :elements    {:e1 {:required    true,
                                                                       :description "Some extension 1 text",
                                                                       :type        "string"},
                                                                  :e2 {:collection true,
                                                                       :minItems   1,
                                                                       :type       "Type",
                                                                       :valueset   {:id "some-ext"}}}}},
       [:Patient :identifier]         {:minItems 1},
       [:Patient :identifier :system] {:required true},
       [:Patient :identifier :value]  {:required    true,
                                       :description "The value that is unique within the system"},
       [:Patient :name]               {:minItems 1, :description "A name associated with the patient",
                                       :rule     {:r1 "family.count() + given.count() >= 1"}},
       [:Patient :name :family]       {},
       [:Patient :name :given]        {}}))
  ;
  (def domain-resource
    (ordered-map {[:Patient :address]                 {},
                  [:Patient :address :line]           {},
                  [:Patient :address :city]           {},
                  [:Patient :address :postalCode]     {:description "US Zip Codes"},
                  [:Patient :address :state]          {:valueset {:id "us-core-usps-state"}},
                  [:Patient :extension:race]          {:sliceName           "race",
                                                       :extension-reference "race"},
                  [:Patient :name]                    {:minItems    1,
                                                       :description "A name associated with the patient",
                                                       :rule        {:r1 "family.count () + given.count () >= 1"}},
                  [:Patient :name :family]            {},
                  [:Patient :name :given]             {},
                  [:Patient :birthDate]               {},
                  [:Patient :communication]           {},
                  [:Patient :communication :language] {:required true, :valueset {:id "simple-language"}},
                  [:Patient :identifier]              {:minItems 1},
                  [:Patient :identifier :system]      {:required true},
                  [:Patient :identifier :value]       {:required    true,
                                                       :description "The value that is unique within the system"},
                  [:Patient :extension:birthsex]      {:sliceName           "birthsex",
                                                       :extension-reference "birthsex"},
                  [:Patient :telecom]                 {},
                  [:Patient :telecom :value]          {:required true},
                  [:Patient :telecom :system]         {:required true, :valueset {:id "fhir:contact-point-system"}},
                  [:Patient :gender]                  {:valueset {:id "fhir:administrative-gender"}}}))

  (testing "domain resource parsing"
    (matcho/match
      (prof/translate
        json-rule/agenda
        :prof-id
        (into (ordered-map []) domain-resource))
      [{:id   "Patient.address.postalCode",
        :path "Patient.address.postalCode"}
       {:id   "Patient.identifier.system",
        :path "Patient.identifier.system",
        :min  1}
       {:id   "Patient.telecom.value",
        :path "Patient.telecom.value",
        :min  1}
       {:id   "Patient.address",
        :path "Patient.address"}
       {:id      "Patient.address.state",
        :path    "Patient.address.state",
        :binding {:strength "required",
                  :valueSet "http://hl7.org/fhir/ValueSet/administrative-gender"}}
       {:id   "Patient.name.given",
        :path "Patient.name.given"}
       {:id        "Patient.extension:birthsex",
        :path      "Patient.extension:birthsex",
        :sliceName "birthsex",
        :type      [{:code    "Extension",
                     :profile ["http://hl7.org/fhir/us/core/StructureDefinition/prof-id-birthsex"]}]}
       {:id   "Patient.address.line",
        :path "Patient.address.line"}
       {:id   "Patient.identifier",
        :path "Patient.identifier",
        :min  1}
       {:id        "Patient.extension:race",
        :path      "Patient.extension:race",
        :sliceName "race",
        :type      [{:code    "Extension",
                     :profile ["http://hl7.org/fhir/us/core/StructureDefinition/prof-id-race"]}]}
       {:id      "Patient.gender",
        :path    "Patient.gender",
        :binding {:strength "required",
                  :valueSet "http://hl7.org/fhir/ValueSet/administrative-gender"}}
       {:id   "Patient.identifier.value",
        :path "Patient.identifier.value",
        :min  1}
       {:id "Patient.telecom", :path "Patient.telecom"}
       {:id      "Patient.communication.language",
        :path    "Patient.communication.language",
        :binding {:strength "required",
                  :valueSet "http://hl7.org/fhir/ValueSet/administrative-gender"},
        :min     1}
       {:id "Patient.communication", :path "Patient.communication"}
       {:id "Patient.address.city", :path "Patient.address.city"}
       {:id      "Patient.telecom.system",
        :path    "Patient.telecom.system",
        :binding {:strength "required",
                  :valueSet "http://hl7.org/fhir/ValueSet/administrative-gender"},
        :min     1}
       {:id "Patient.birthDate", :path "Patient.birthDate"}
       {:id "Patient.name.family", :path "Patient.name.family"}
       {:id "Patient.name", :path "Patient.name", :min 1}])))

