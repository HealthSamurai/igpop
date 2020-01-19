(ns igpop.profile.profile-test
  (:require [igpop.profile.profile :as prof]
            [clojure.pprint :as p]
            [clojure.test :refer :all]
            [matcho.core :as matcho]
            [clojure.java.io :as io]
            [igpop.loader :as loader]
            [cheshire.core :refer :all]
            [flatland.ordered.map :refer :all]))

(deftest test-profiles

  (def profiles {:Patient {:profile-id {:elements    {:identifier {:a 1 :b 2}
                                                      :extension  {:ex-1 1
                                                                   :ex-2 2}}
                                        :description "some description"}}})

  (testing "test extract resources to parse"
    (matcho/match
      (prof/extract-resources-to-parse :Patient :profile-id profiles)
      [{:elements     {:identifier {:a 1 :b 2}
                       :extension  {:ex-1 1 :ex-2 2}}
        :profile-type :Patient
        :type         :DomainResource}
       {:elements     {:ex-1 1}
        :profile-type :Extension
        :type         :Extension}
       {:elements     {:ex-2 2}
        :profile-type :Extension
        :type         :Extension}]))

  (def elements {
                 :extension  {:extension1 {:description "Some extension 1"
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
      (prof/flatten-profile elements [:Patient])
      (ordered-map {[:Patient :extension :extension1] {:extension {:name  "extension1"
                                                                   :id    [:Patient :extension :extension1]
                                                                   :entry {:description "Some extension 1"
                                                                           :elements    {:e1 {:required    true
                                                                                              :description "Some extension 1 text"
                                                                                              :type        "string"}
                                                                                         :e2 {:collection true
                                                                                              :minItems   1
                                                                                              :type       "Type"
                                                                                              :valueset   {:id "some-ext"}}}}}}
                    [:Patient :identifier]            {:minItems 1}
                    [:Patient :identifier :system]    {:required true}
                    [:Patient :identifier :value]     {:required    true
                                                       :description "The value that is unique within the system"}
                    [:Patient :name]                  {:minItems    1
                                                       :description "A name associated with the patient"
                                                       :rule        {:r1 "family.count() + given.count() >= 1"}}
                    [:Patient :name :family]          {} [:Patient :name :given] {}})))


  (def domain-resource
    (ordered-map {
                  [:Patient :extension :extension1] {:extension {:name  "extension1"
                                                                 :id    [:Patient :extension :extension1]
                                                                 :entry {:description "Some extension 1"
                                                                         :elements    {:e1 {:required    true
                                                                                            :description "Some extension 1 text"
                                                                                            :type        "string"}
                                                                                       :e2 {:collection true
                                                                                            :minItems   1
                                                                                            :type       "Type"
                                                                                            :valueset   {:id "some-ext"}}}}}}
                  [:Patient :identifier]            {:minItems 1}
                  [:Patient :identifier :system]    {:required true}
                  [:Patient :identifier :value]     {:required    true
                                                     :description "The value that is unique within the system"}
                  [:Patient :name]                  {:minItems    1
                                                     :description "A name associated with the patient"
                                                     :rule        {:r1 "family.count() + given.count() >= 1"}}
                  [:Patient :name :family]          {} [:Patient :name :given] {}}))


  (testing "domain resource parsing"
    (matcho/match
      (prof/domain-resource->structure-definition (into (ordered-map []) domain-resource))
      [(ordered-map [[:id [:Patient :extension :extension1]]
                     [:path [:Patient :extension :extension1]]
                     [:mustSupport true]
                     [:sliceName "extension1"]
                     [:min 0]
                     [:max "1"]
                     [:type [{:code    "Extension"
                              :profile ["http://hl7.org/fhir/us/core/StructureDefinition/extension1"]}]]
                     [:mapping [{:identity "argonaut-dq-dstu2" :map [:Patient :extension :extension1]}]]])
       (ordered-map [[:id "Patient.identifier"]
                     [:path "Patient.identifier"]
                     [:mustSupport true]
                     [:min 1]])
       (ordered-map [[:id "Patient.identifier.system"]
                     [:path "Patient.identifier.system"]
                     [:mustSupport true]
                     [:min 1]])
       (ordered-map [[:id "Patient.identifier.value"]
                     [:path "Patient.identifier.value"]
                     [:mustSupport true]
                     [:min 1]])
       (ordered-map [[:id "Patient.name"]
                     [:path "Patient.name"]
                     [:mustSupport true]
                     [:min 1]])
       (ordered-map [[:id "Patient.name.family"]
                     [:path "Patient.name.family"]
                     [:mustSupport true]])
       (ordered-map [[:id "Patient.name.given"]
                     [:path "Patient.name.given"]
                     [:mustSupport true]])]))


  (def extension-diff
    {[:Extension :race]              {:description "US Core Race Extension"}
     [:Extension :race :text]        {:required    true
                                      :description "Race Text"
                                      :type        "string"}
     [:Extension :race :ombCategory] {:collection true
                                      :minItems   1
                                      :type       "Coding"
                                      :valueset   {:id "omb-race-category"}}

     [:Extension :race :detailed]    {:collection  true
                                      :description "Extended race codes"
                                      :type        "Coding"
                                      :valueset    {:id "detailed-race"}}})

  (testing "extension diff parsing"
    (matcho/match
      (prof/extension-diff->structure-definition (into (ordered-map []) extension-diff))
      [{:id         "Extension"
        :path       "Extension"
        :short      "Extension"
        :definition "US Core Race Extension"
        :min        0
        :max        "1"}
       {:id       "Extension.url"
        :path     "Extension.url"
        :min      1
        :max      "1"
        :type     [{:code "uri"}]
        :fixedUri "race"}
       {:id   "Extension.value[x]"
        :path "Extension.value[x]"
        :min  0
        :max  "0"}
       {:id          "Extension.extension:text"
        :path        "Extension.extension"
        :sliceName   "text"
        :min         0
        :max         "1"
        :type        [{:code "Extension"}]
        :mustSupport true
        :mapping     [{:map "race"}]}
       {:id       "Extension.extension:text.url"
        :path     "Extension.extension.url"
        :min      1 :max "1"
        :type     [{:code "uri"}]
        :fixedUri "race"}
       {:id      "Extension.extension:text.valueString"
        :path    "Extension.extension.valueString"
        :min     1
        :max     "1"
        :type    [{:code "String"}]
        :binding {:strength    true
                  :description "Race Text"
                  :valueSet    "http://hl7.org/fhir/us/core/ValueSet/omb-race-category"}}
       {:id          "Extension.extension:ombCategory"
        :path        "Extension.extension"
        :sliceName   "ombCategory"
        :min         0
        :max         "1"
        :type        [{:code "Extension"}]
        :mustSupport true
        :mapping     [{:map "race"}]}
       {:id       "Extension.extension:ombCategory.url"
        :path     "Extension.extension.url"
        :min      1 :max "1"
        :type     [{:code "uri"}]
        :fixedUri "race"}
       {:id      "Extension.extension:ombCategory.valueCoding"
        :path    "Extension.extension.valueCoding"
        :min     1
        :max     "1"
        :type    [{:code "Coding"}]
        :binding {:strength    nil
                  :description nil
                  :valueSet    "http://hl7.org/fhir/us/core/ValueSet/omb-race-category"}}
       {:id          "Extension.extension:detailed"
        :path        "Extension.extension"
        :sliceName   "detailed"
        :min         0
        :max         "1"
        :type        [{:code "Extension"}]
        :mustSupport true
        :mapping     [{:map "race"}]}
       {:id       "Extension.extension:detailed.url"
        :path     "Extension.extension.url"
        :min      1
        :max      "1"
        :type     [{:code "uri"}]
        :fixedUri "race"}
       {:id      "Extension.extension:detailed.valueCoding"
        :path    "Extension.extension.valueCoding"
        :min     1
        :max     "1"
        :type    [{:code "Coding"}]
        :binding {:strength    nil
                  :description "Extended race codes"
                  :valueSet    "http://hl7.org/fhir/us/core/ValueSet/omb-race-category"}}]))
  )

