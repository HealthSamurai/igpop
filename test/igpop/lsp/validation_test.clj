(ns igpop.lsp.validation-test
  (:require [igpop.lsp.validation :as sut]
            [igpop.parser]
            [clojure.test :refer :all]
            [matcho.core :as matcho]
            [zprint.core :as zp]))

(deftest lsp-validation-test
  (def ctx
    {:manifest 
     {:base
      {:profiles {:Patient {:elements {:name {:type "HumanName" :collection true}}}
                  :HumanName {:elements {:given {:type "string" :collection true}}}}}}})

  (def ast
    (igpop.parser/parse "
elements:
  birthDate:
    maxItems: 4
  broken
  name:
    required: true
    elements:
      given:
        maxItems: 1
      wrong: key
"))

  ;; (zp/zprint ast)

  (def value (sut/collect-value ast))
  (:value value)

  (matcho/match
   value
   {:idx {[:elements] {:from {} :to {}}
          [:elements :name] {:from {} :to {}}
          [:elements :name :required] {:from {} :to {}}
          [:elements :birthDate] {:from {} :to {}}
          [:elements :birthDate :maxItems] {:from {} :to {}}}
    :value {:elements {:birthDate {:maxItems 4}
                       :name {:elements {:wrong "key"}
                              :birthDate nil?}}}})

  (def errs (sut/validate-profile ctx :Patient (:value value)))
  (matcho/match
   errs
   [{:path [:elements :birthDate]}
    {:path [:elements :name :elements :wrong]}
    nil?])


  (sut/errors-to-diagnostic (:idx value) errs)

  (sut/structure-validation ctx :Patient ast)




  )

