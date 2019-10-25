(ns profile-site.utils-test
  (:require [profile-site.utils :as sut]
            [clojure.test :refer :all]))

(deftest vector-first-path-test
  (testing "Should return a correct path"
    (is (= [2 3 0] (sut/vector-first-path #(= % 3) [1 2 [4 5 6 [3 4]]])))))

