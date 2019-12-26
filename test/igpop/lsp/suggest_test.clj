(ns igpop.lsp.suggest-test
  (:require [igpop.lsp.suggest :as sut]
            [igpop.parser]
            [zprint.core :as zp]
            [clojure.test :refer :all]))


(deftest test-lsp-suggest
  (def ast (igpop.parser/parse "elements:\n  n"))

  (zp/zprint
   (sut/pos-to-path ast {:ln 1 :pos 3}))


  (zp/zprint
   (sut/suggest {} :Patient ast {:ln 1 :pos 3}))


  (sut/suggest {}
               :Patient
               (igpop.parser/parse "elements:\n  name:\n    " {})
               {:ln 2 :pos 4})

  (def ast (igpop.parser/parse "elements:\n  name:\n    m"))

  (zp/zprint ast)

  (sut/suggest {} :Patient ast {:ln 2 :pos 4})





  )

