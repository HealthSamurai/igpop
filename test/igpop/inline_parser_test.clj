(ns igpop.inline-parser-test
  (:require
   [igpop.inline-parser :as sut]
   [matcho.core :as matcho]
   [clojure.test :refer :all]
   [zprint.core :as zp]))

(deftest test-inline-parser 
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

    (matcho/match
     (sut/parse-inline {:text "  " :pos 0 :ln 0})
     {:type :empty})

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

    (matcho/match
     (sut/do-read "{a: 1, b: 'hello'" 0 1)
     [{:kind :inline,
       :type :map,
       :error "Expected } to close map"
       :value
       [{,:key :a,
         :kind :inline,
         :type :kv,
         :value {:type :int,:value 1}}
        {:key :b,
         :kind :inline,
         :type :kv,
         :value {:type :str,:value "hello"}}]}])

    (matcho/match
     (sut/do-read "{ valueset { id : x}}" 0 1)
     [{:type :map
       :value [{:type :kv
                :key :valueset
                :value {:type :map
                        :value [{:type :kv
                                 :key :id
                                 :value {:value "x"}}]}}]}])




    ))
