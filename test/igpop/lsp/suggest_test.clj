(ns igpop.lsp.suggest-test
  (:require [igpop.lsp.suggest :as sut]
            [igpop.parser]
            [zprint.core :as zp]
            [matcho.core :as matcho]
            [clojure.test :refer :all]))

;; integrate loader

;; * suggest elements for resource type
;;   for example name for Patient
;;   * suggest complex type elements
;;   for example family for Patient.name
;; * suggest igpop keys
;;   for example Patient.elements.name => minItems maxItes
;;   distinguish collections and non-collections keys
;; * suggest valueset ids
;; * suggest types
;;   Patient.elements.extensions.race => Coding, code, .....


;; for hover
;;   hover for igpop keys
;;   hover for FHIR fields


;; go to definition for valuset

;; type or elements

;; examples:
;; [:Patient] [ ]
;; - [igpop-schema] suggest props
;; [:Patient :description] [ ]
;;   - [base] suggest val.
;; [:Patient :elements] [x]
;;   - [base] suggest -elements name-. [id meta language ...etc]
;;   - [igpop-schema] suggest "extension"
;; [:Patient :elements :identifier] [ ]
;;   - [igpop-schem] suggest props for element. [need some smartness here (minItem for coll etc...)]
;; [:Patient :elements :birthsex :valueset] [ ]
;;   - [vs] { id: 'fhir:administrative-gender' }


(def ctx
  {:manifest {:base {:profiles
                     {:Patient {:elements {:name {:type "HumanName"
                                                  :description "Some Description"
                                                  :isCollection true}
                                           :birthDate {:description "The date of birth for the individual"
                                                       :type "date"}
                                           :contact {:collection true
                                                     :description "description"
                                                     :elements {:id {
                                                                     :description "uniq id"
                                                                     :type "string"
                                                                     }
                                                                :name {
                                                                       :description "contact person name"
                                                                       :type "HumanName"
                                                                       }
                                                                }}
                                           :address {:type "Address"
                                                     :collection true
                                                     :description "An address for the individual"}}}
                      :Address {:elements {:line {:type "string"}
                                           :period {:type "Period"}}}}}
              :primitive-types {}
              :valuesets {:id {}}

              :schema {:description
                        {:description "FHIR profile description"
                         :Type "String"}
                        :elements
                        {:Type "Map"
                         :description "Definition of elements"
                         :value {:type  { :Type "code", :description "Element type" }
                                 :description { :Type "string", :description "Element description"}
                                 :elements { :ref "elements", :description "Nested elements"}
                                 :valueset {:id { :Type "string", :description "Element type" }
                                            :url { :Type "url" }
                                            :strength {:Type "code"
                                                       :enum ["extensible" "required"]
                                                       :default "extensible"}}
                                 :disabled { :Type "boolean" }
                                 :collection { :Type "boolean" }
                                 :minItems { :Type "integer", :for "??collection"}
                                 :maxItems { :Type "integer" }
                                 :mustSupport {:Type "boolean" :default true}
                                 }
                         }
                       }

              }})




(deftest test-lsp-suggest
  (matcho/match
   (sut/sgst-elements-name ctx [:Patient :elements] nil)
   [{:label "name:"
     :detail "Some Description"}
    {:label "birthDate:"
     :detail "The date of birth for the individual"}
    ])

  (matcho/match
   (sut/sgst-elements-name ctx [:Patient :elements :contact :elements] nil)
   [{:label "id:"
     :detail "uniq id"}
    {:label "name:"
     :detail "contact person name"}
    {:label "extension:"
     :detail "Additional content defined by implementations"}
    ])


  (matcho/match
   (sut/sgst-igpop-keys ctx [:Patient] nil)
   [{:label "description:"}
    {:label "elements:"}]
   )

  (is (empty? (sut/sgst-igpop-keys ctx [:Patient :elements] nil)))


  (matcho/match
   (sut/sgst-igpop-keys ctx [:Patient :elements :identifier] nil)
   [{:label "description:"}
    {:label "disabled:"}]
   )

  (matcho/match
   (sut/sgst-igpop-keys ctx [:Patient :elements :identifier :valueset] nil)
   [{:label "id:", :kind 12, :detail "Element type"}
    {:label "url:", :kind 12, :detail nil}
    {:label "strength:", :kind 12, :detail nil}]
   )

  (matcho/match
   (sut/sgst-igpop-keys ctx [:Patient :elements :address :elements :postalCode] nil)
   [{:label "description:"}
    {:label "disabled:"}]
   )

  (matcho/match
   (sut/sgst-igpop-keys ctx [:Patient :elements :address :elements :postalCode :valueset] nil)
   [{:label "id:", :kind 12, :detail "Element type"}
    {:label "url:", :kind 12, :detail nil}
    {:label "strength:", :kind 12, :detail nil}]
   )

  (matcho/match
   (sut/sgst-complex-types ctx [:Patient :elements :address :elements] nil)
   [{:label "line:"}
    {:label "period:"}])


  )



(comment
 (sut/sgst-igpop-keys ctx [:Patient] nil)

 (sut/sgst-igpop-keys ctx [:Patient :elements :address :elements] nil)

 (sut/sgst-igpop-keys ctx [:Patient :elements] nil)

 (sut/sgst-igpop-keys ctx [:Patient :elements :name] nil)


 (sut/sgst-igpop-keys ctx [:Patient :elements :name :valueset] nil)

 (get-in ctx [:manifest :schema :elements :elements :value :elements :valueset])

 (sut/sgst-igpop-keys ctx [:Patient :elements :identifier] nil)

 (sut/sgst-igpop-keys ctx [:Patient :elements :address :elements :postalCode] nil)

 (sut/sgst-elements-name ctx [:Patient :elements :address :elements] nil)

 (sut/sgst-complex-types ctx [:Patient :elements :address :elements] nil)

)

