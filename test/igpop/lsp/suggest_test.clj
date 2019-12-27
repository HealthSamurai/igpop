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

;; examples:
;; [:Patient] [ ]
;; - [igpop-schema] suggest props
;; [:Patient :description] [ ]
;;   - [base] suggest val.
;; [:Patient :elements] [ ]
;;   - [base] suggest -elements name-. [id meta language ...etc]
;;   - [igpop-schema] suggest "extension"
;; [:Patient :elements :identifier] [ ]
;;   - [igpop-schem] suggest props for element. [need some smartness here (minItem for coll etc...)]
;; [:Patient :elements :birthsex :valueset] [ ]
;;   - [vs] { id: 'fhir:administrative-gender' }


(def ctx
  {:manifest {:base
              {:profiles
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
                                                          }}}}
                :HumanName {:elements {:family {:type "string"}}}}}
              :primitive-types {}
              :valuesets {:id {}}}})




(deftest test-lsp-suggest
  (matcho/match
   (sut/sgst-elements-name ctx [:Patient :elements] nil)
   [{:label "name: "
     :detail "Some Description"}
    {:label "birthDate: "
     :detail "The date of birth for the individual"}
    ])

  (matcho/match
   (sut/sgst-elements-name ctx [:Patient :elements :contact :elements] nil)
   [{:label "id: "
     :detail "uniq id"}
    {:label "name: "
     :detail "contact person name"}
    ])

  )