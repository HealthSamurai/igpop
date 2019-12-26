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

  (testing "parse-entry"


    (matcho/match
     (sut/parse-entry {:ln 4 :pos 0 :ident 0 :text "key: value"})
     {:type :kv
      :kind :inline
      :value {:type :str
              :value "value"}})

    (matcho/match
     (sut/parse-entry {:ln 4 :pos 0 :ident 0 :text ""})
     {:type :kv
      :kind :newline})


    (matcho/match
     (sut/parse-entry {:ln 4 :pos 0 :ident 0 :text "key"})
     {:type :kv
      :key "key"
      :kind :key-start})

    (matcho/match
     (sut/parse-entry {:ln 4 :pos 0 :ident 0 :text "key:"})
     {:type :kv
      :key :key
      :kind :block})

    (matcho/match
     (sut/parse-entry {:ln 4 :pos 0 :ident 0 :text "key: |"})
     {:type :kv
      :key :key
      :kind :text-multiline})


    )

  (println "\n\n")
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
             :kind :newline}
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
             :block {:from {:ln 0, :pos 0}, :to {:ln 0, :pos 5}}}]})


  (matcho/match
   (sut/parse "elements:\n  nam" {:start :map})
   {:block {:from {:ln 0, :pos 0}, :to {:ln 1, :pos 5}},
    :type :map,
    :value
    [{:block {:from {:ln 0, :pos 0}, :to {:ln 1, :pos 5}},
      :key :elements,
      :kind :block,
      :type :kv,
      :value
      {:block {:from {:ln 1, :pos 2}, :to {:ln 1, :pos 5}},
       :type :map,
       :value [{:block {:from {:ln 1, :pos 2}, :to {:ln 1, :pos 5}},
                :key "nam",
                :kind :key-start,
                :type :kv}]}}]})

  ;; TODO: important for suggest
  (matcho/match
   (sut/parse "elements:\n  name:\n    " {})
   {:block {:from {:ln 0, :pos 0}, :to {}},
    :type :map,
    :value
    [{:block {:from {:ln 0, :pos 0}, :to {}},
      :key :elements,
      :type :kv,
      :value
      {:block {:from {:ln 1, :pos 2}, :to {}},
       :type :map,
       :value [{:block {:from {:ln 1, :pos 2}, :to {:ln number? :pos number?}},
                :key :name,
                :type :kv,
                :value {:type :map
                        :value [{:kind :newline}]}}]}}]})


  (zp/zprint
   (sut/parse "elements:\n  name:\n    minIt" {}))


  (matcho/match
   (sut/parse "name: Nikolai\ngiv\nfamily: Ryzhikov")
   {:block {:from {:ln 0, :pos 0}, :to {:ln 2, :pos 16}},
    :type :map,
    :value
    [{:block {:from {:ln 0, :pos 0}, :to {:ln 0, :pos 13}},
      :key :name,
      :kind :inline,
      :type :kv,
      :value {:block {:from {:ln 0, :pos 5}, :to {:ln 0, :pos 13}},
              :type :str,
              :value "Nikolai"}}
     {:block {:from {:ln 1, :pos 0}, :to {:ln 1, :pos 3}},
      :key "giv",
      :kind :key-start,
      :type :kv}
     {:block {:from {:ln 2, :pos 0}, :to {:ln 2, :pos 16}},
      :key :family,
      :kind :inline,
      :type :kv,
      :value {:block {:from {:ln 2, :pos 7}, :to {:ln 2, :pos 16}},
              :type :str,
              :value "Ryzhikov"}}]}
   )

  )

