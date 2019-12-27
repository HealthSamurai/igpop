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
   (sut/parse "desc: |\n  line1\n  line2\n  line3")
   {:block {:from {:ln 0, :pos 0}, :to {:ln 3, :pos 2}},
    :type :map,
    :value
    [{:block {:from {:ln 0, :pos 0}, :to {:ln 3, :pos 2}},
      :key :desc,
      :kind :text-multiline,
      :type :kv,
      :value {:block {:from {:ln 1, :pos 2}, :to {:ln 3, :pos 2}},
              :type :str,
              :value "line1\nline2\nline3"}}]})

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


  ;; (zp/zprint
  ;;  (sut/parse "elements:\n  name:\n    minIt" {}))


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



  (matcho/match
   (sut/parse "- 1\n- 2" {})
   {:type :coll
    :block {:from {:ln 0 :pos 0}
            :to {:ln 1 :pos 2}}
    :value
    [{:type :coll-entry,
      :block {:from {:ln 0 :pos 0}
              :to   {:ln 0 :pos 2}}
      :value {:block {:from {:ln 0, :pos 1},
                      :to {:ln 0, :pos 2}},
              :value 1}}
     {:type :coll-entry,
      :block {:from {:ln 1 :pos 0}
              :to {:ln 1 :pos 2}}
      :value {:block {:from {:ln 1, :pos 1},
                      :to {:ln 1, :pos 2}},
              :type :int
              :value 2}}]})


  (matcho/match
   (sut/parse "- a: a\n  b: b\n- a: a\n  b: b" {})

   {:block {:from {:ln 0, :pos 0}, :to {:ln 3, :pos 6}},
    :type :coll,
    :value
    [{:block {:from {:ln 0, :pos 0}, :to {:ln 1, :pos 6}},
      :type :coll-entry,
      :value
      {:block {:from {:ln 0, :pos 1}, :to {:ln 1, :pos 6}},
       :type :map,
       :value
       [{:block {:from {:ln 0, :pos 1}, :to {:ln 0, :pos 5}},
         :key :a,
         :kind :inline,
         :type :kv,
         :value {:block {:from {:ln 0, :pos 3}, :to {:ln 0, :pos 5}},
                 :type :str,
                 :value "a"}}
        {:block {:from {:ln 1, :pos 2}, :to {:ln 1, :pos 6}},
         :key :b,
         :kind :inline,
         :type :kv,
         :value {:block {:from {:ln 1, :pos 4}, :to {:ln 1, :pos 6}},
                 :type :str,
                 :value "b"}}]}}
     {:block {:from {:ln 2, :pos 0}, :to {:ln 3, :pos 6}},
      :type :coll-entry,
      :value
      {:block {:from {:ln 2, :pos 1}, :to {:ln 3, :pos 6}},
       :type :map,
       :value
       [{:block {:from {:ln 2, :pos 1}, :to {:ln 2, :pos 5}},
         :key :a,
         :kind :inline,
         :type :kv,
         :value {:block {:from {:ln 2, :pos 3}, :to {:ln 2, :pos 5}},
                 :type :str,
                 :value "a"}}
        {:block {:from {:ln 3, :pos 2}, :to {:ln 3, :pos 6}},
         :key :b,
         :kind :inline,
         :type :kv,
         :value {:block {:from {:ln 3, :pos 4}, :to {:ln 3, :pos 6}},
                 :type :str,
                 :value "b"}}]}}]})





  (matcho/match
   (sut/parse "aaa")
   {:type :map
    :value [{:type :kv
             :kind :key-start
             :error "Expected key closed by ':'"}]})

  (matcho/match
   (sut/errors
    {:type :map
     :value [{:type :kv
              :kind :key-start
              :block {:from "from" :to "to"}
              :error "Expected key closed by ':'"}]}))




  (matcho/match
   (sut/errors
    {:block {:from {:ln 0, :pos 0}, :to {:ln 6, :pos 7}},
     :type :map,
     :value
     [{:block {:from {:ln 0, :pos 0}, :to {:ln 5, :pos 8}},
       :key :elements,
       :kind :block,
       :type :kv,
       :value
       {:block {:from {:ln 1, :pos 2}, :to {:ln 5, :pos 8}},
        :type :map,
        :value
        [{:block {:from {:ln 1, :pos 2}, :to {:ln 1, :pos 6}},
          :error "Expected key closed by ':'",
          :key "upso",
          :kind :key-start,
          :type :kv}
         {:block {:from {:ln 2, :pos 2}, :to {:ln 4, :pos 12}},
          :key :name,
          :kind :block,
          :type :kv,
          :value
          {:block {:from {:ln 3, :pos 4}, :to {:ln 4, :pos 12}},
           :type :map,
           :value
           [{:block {:from {:ln 3, :pos 4}, :to {:ln 3, :pos 15}},
             :key :maxItems,
             :kind :inline,
             :type :kv,
             :value {:block {:from {:ln 3, :pos 13},
                             :to {:ln 3, :pos 15}},
                     :type :str,
                     :value "1"}}
            {:block {:from {:ln 4, :pos 4}, :to {:ln 4, :pos 12}},
             :error "Expected key closed by ':'",
             :key "minItems",
             :kind :key-start,
             :type :kv}]}}
         {:block {:from {:ln 5, :pos 2}, :to {:ln 5, :pos 8}},
          :error "Expected key closed by ':'",
          :key "badkey",
          :kind :key-start,
          :type :kv}]}}
      {:block {:from {:ln 6, :pos 0}, :to {:ln 6, :pos 7}},
       :key :badkey,
       :kind :inline,
       :type :kv}]})
   [{:block {:from {:ln 1, :pos 2}, :to {:ln 1, :pos 6}},
     :message "Expected key closed by ':'"}
    {:block {:from {:ln 4, :pos 4}, :to {:ln 4, :pos 12}},
     :message "Expected key closed by ':'"}
    {:block {:from {:ln 5, :pos 2}, :to {:ln 5, :pos 8}},
     :message "Expected key closed by ':'"}]
   )

  (testing "inline parse"

    (is (= (sut/inline-type "{}") :map))
    (is (= (sut/inline-type "[]") :coll))
    (is (= (sut/inline-type "'aaa'") :str-q))
    (is (= (sut/inline-type "\"bbb\"") :str-dq))
    (is (= (sut/inline-type " any text") :alphanum))
    (is (= (sut/inline-type "10") :int))
    (is (= (sut/inline-type "10.01") :num))
    (is (= (sut/inline-type "true") :true))
    (is (= (sut/inline-type "false") :false))
    (is (= (sut/inline-type "null") :null))

    (sut/parse-inline {:text "{}" :pos 0 :ln 0})

    (matcho/match
     (sut/do-read "100" 0 1)
     [{:type :int
       :value 100}])

    (matcho/match
     (sut/do-read "\"abc\"" 0 1)
     [{:type :str
       :value "abc"}])

    (matcho/match
     (sut/do-read "\"abc " 0 1)
     [{:type :str
       :error string?
       :value "\"abc "}
      nil?])

    (matcho/match
     (sut/do-read " true rest" 0 1)
     [{:type :bool
       :value true}
      " rest"])

    (matcho/match
     (sut/do-read " false rest" 0 1)
     [{:type :bool
       :value false}
      " rest"])

    (matcho/match
     (sut/read-inline :key "abc: 1" 0 1)
     [{:type :key
       :value :abc}
      " 1"])

    (matcho/match
     (sut/read-inline :key "  abc: 1" 0 1)
     [{:type :key
       :value :abc}
      " 1"])
    

    (matcho/match
     (sut/do-read " { }" 0 1)
     [{:type :map
       :kind :empty} nil])

    (matcho/match
     (sut/do-read "{a: 1}" 0 1)
     [{:type :map
       :value [{:type :kv
                :key :a
                :value {:value 1}}]}])

    (matcho/match
     (sut/do-read "{a: 1, b: hello}" 0 1)
     [{:type :map
       :value [{:type :kv :key :a
                :value {:value 1}}
               {:type :kv :key :b
                :value {:value "hello"}}]}])




    )


  ;; (let [s (slurp "test/igpop/parser/basic.yaml")]
  ;;   (time (sut/parse s))
  ;;   #_(zp/zprint (sut/parse s)))

  )



