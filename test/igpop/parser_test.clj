(ns igpop.parser-test
  (:require [igpop.parser :as sut]
            [clojure.test :refer :all]
            [clj-yaml.core]
            [zprint.core :as zp]
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
                     :to   {:ln 0 :pos 12}}
             :value {:type :str
                     :value "nicola"
                     :block {:from {:ln 0 :pos 5}
                             :to {:ln 0 :pos 12}}}}
            {:type :kv
             :key :given
             :block {:from {:ln 1 :pos 0}
                     :to   {:ln 1 :pos 12}}
             :value {:type :str
                     :value "hello"
                     :block {:from {:ln 1 :pos 6}
                             :to {:ln 1 :pos 12}}}}]})


  (matcho/match
   (sut/parse "name: nicola\n\ngiven: hello")
   {:type :map
    :block {:from {:ln 0 :pos 0}
            :to {:ln 2 :pos 12}}
    :value [{:type :kv
             :block {:from {:ln 0 :pos 0}
                     :to {:ln 0 :pos 12}}
             :value {:type :str
                    :value "nicola"
                    :block {:from {:ln 0 :pos 5}
                            :to {:ln 0 :pos 12}}}}
            {:type :kv
             :block {:from {:ln 2 :pos 0}
                     :to   {:ln 2 :pos 12}}
             :value {:type :str
                     :value "hello"
                     :block {:from {:ln 2 :pos 6}
                             :to {:ln 2 :pos 12}}}}]})


  (matcho/match
   (sut/parse "elements:\n  name: nicola\n  given: hello")
   {:block {:from {:ln 0, :pos 0}, :to {:ln 2, :pos 14}},
    :type :map,
    :value
    [{:block {:from {:ln 0, :pos 0}, :to {:ln 2, :pos 14}},
      :key :elements,
      :type :kv,
      :value
      {:block {:from {:ln 1, :pos 2}, :to {:ln 2, :pos 14}},
       :type :map,
       :value
       [{:block {:from {:ln 1, :pos 2}, :to {:ln 1, :pos 14}},
         :key :name,
         :type :kv,
         :value {:block {:from {:ln 1, :pos 7}, :to {:ln 1, :pos 14}},
                 :type :str,
                 :value "nicola"}}
        {:block {:from {:ln 2, :pos 2}, :to {:ln 2, :pos 14}},
         :key :given,
         :type :kv,
         :value {:block {:from {:ln 2, :pos 8}, :to {:ln 2, :pos 14}},
                 :type :str,
                 :value "hello"}}]}}]})

  (matcho/match
   (sut/parse "eleme" {:start :map})
   {:block {:from {:ln 0, :pos 0}, :to {:ln 0, :pos 5}},
    :type :map,
    :value [{:type :kv
             :key "eleme"
             :block {:from {:ln 0, :pos 0}, :to {:ln 0, :pos 5}}
             :invalid true}]}
   )


  (zp/zprint
   (sut/parse "elements:\n  nam" {:start :map}))

  ;; TODO: important for suggest
  (zp/zprint
   (sut/parse "elements:\n  name:\n    " {}))


  (zp/zprint
   (sut/parse "elements:\n  name:\n    minIt" {}))


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

