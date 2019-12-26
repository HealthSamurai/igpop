(ns igpop.parser-test
  (:require [igpop.parser :as sut]
            [clojure.test :refer :all]
            [clojure.string :as str]
            [matcho.core :as matcho]))


(deftest test-igpop-parser

  (matcho/match
   (sut/parse-lines "a: 1
b: 2
elements:
  name: 1
  given: 2")
   [{:ln 0, :ident 0, :text "a: 1"}
    {:ln 1, :ident 0, :text "b: 2"}
    {:ln 2, :ident 0, :text "elements:"}
    {:ln 3, :ident 2, :text "name: 1"}
    {:ln 4, :ident 2, :text "given: 2"}]
   )

  (matcho/match
   (sut/parse "name: nicola\ngiven: hello")
   {:type :map
    :block {:from {:ln 0 :pos 0}
            :to {:ln 1 :pos 12}}
    :value [{:type :kv
             :key :name
             :block {:from {:ln 0 :pos 0}
                     :to {:ln 0 :pos 11}}
             :value {:type :str
                     :value "nicola"
                     :block {:from {:ln 0 :pos 5}
                             :to {:ln 0 :pos 11}}}}
            {:type :kv
             :key :given
             :value {:type :str
                     :value "hello"
                     :block {:from {:ln 1 :pos 6}
                             :to {:ln 1 :pos 11}}}}]})


  ;; (matcho/match
  ;;  (sut/parse "name: nicola\n\ngiven: hello")
  ;;  {:type :map
  ;;   :block {:from {:ln 0 :pos 0}
  ;;           :to {:ln 2 :pos 12}}
  ;;   :value {:name {:type :str
  ;;                  :value "nicola"
  ;;                  :block {:from {:ln 0 :pos 5}
  ;;                          :to {:ln 0 :pos 11}}}
  ;;           :given {:type :str
  ;;                   :value "hello"
  ;;                   :block {:from {:ln 2 :pos 6}
  ;;                           :to {:ln 2 :pos 11}}}}})


  ;; (matcho/match
  ;;  (sut/parse "elements:\n  name: nicola\n  given: hello")
  ;;  {:type :map
  ;;   :block {:from {:ln 0 :pos 0}
  ;;           :to {:ln 2 :pos 14}}
  ;;   :value {:elements {:type :map
  ;;                      :value {:name {:type :str
  ;;                                     :block {:from {:ln 1 :pos 7}
  ;;                                             :to {:ln 1 :pos 13}}}
  ;;                              :given {:type :str
  ;;                                      :block {:from {:ln 2 :pos 8}
  ;;                                              :to {:ln 2 :pos 13}}}}}}})


  ;; (sut/parse "name: Nikolai\ngiv\nfamily: Ryzhikov")

  #_(matcho/match
   (sut/parse "-1\n-2")
   {:type :coll
    :block {:start {:l 0 :p 0}
            :end {:l 1 :p 3}}
    :value [{:type :int
             :value 1
             :block {:start {:l 0 :p 2}
                     :end {:l 0 :p 3}}}
            {:type :int
             :value 2
             :block {:start {:l 1 :p 2}
                     :end {:l 1 :p 3}}}]})

  


  )

