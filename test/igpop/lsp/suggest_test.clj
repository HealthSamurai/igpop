(ns igpop.lsp.suggest-test
  (:require [igpop.lsp.suggest :as sut]
            [igpop.parser]
            [zprint.core :as zp]
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

(def manifest
  {:base-profile
   {:Patient {:elements {:name {:type "HumanName"
                                :isCollection true}}}
    :HumanName {:elements {:family {:type "string"}}}}
   :primitive-types {}
   :valuesets {:id {}}}

  )

(deftest test-lsp-suggest
  (def ast (igpop.parser/parse "elements:\n  n"))

  (zp/zprint
   (sut/pos-to-path ast {:ln 1 :pos 3}))


  (zp/zprint
   (sut/suggest {} :Patient ast {:ln 1 :pos 3}))


  (sut/suggest {:manifest manifest}
               :Patient (igpop.parser/parse "elements:\n  name:\n    " {})
               {:ln 2 :pos 4})

  (def ast (igpop.parser/parse "elements:\n  name:\n    m"))

  (zp/zprint ast)

  (sut/suggest {} :Patient ast {:ln 2 :pos 4})





  )

