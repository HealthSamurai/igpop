(ns loll.core-test
  (:require [loll.core :as sut]
            [matcho.core :as matcho]
            [clojure.test :refer :all]))

(deftest core-test

  (matcho/match
   (sut/validate {}
    {:attrs {:name {:type "string"}}}
    {:namx "Ivan"})
   {:errors [{:message "Unknown element" :path [:namx]}]})


  (matcho/match
   (sut/validate {}
                 {:attrs {:name {:type "string" :req true}}}
                 {})
   {:errors [{:message "Element is required" :path [:name]}]})





  )

