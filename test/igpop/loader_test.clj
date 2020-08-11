(ns igpop.loader-test
  (:require [igpop.loader :as sut]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [matcho.core :as matcho]))

(def project-path (.getPath (io/resource "test-project")))

(deftest read-yaml-test
  (let [result (sut/read-yaml (io/file project-path "ig.yaml"))]
    (is (map? result))
    (is (= "Test project" (:description result)))))

(deftest get-inlined-valuesets-test
  (let [inlined-valueset {:id "inlined-valueset"
                          :concepts [{:code "test1", :display "Test1"}
                                     {:code "test2", :display "Test2"}]}
        test-profile {:basic
                      {:elements
                       {:id {:description "Test id", :type "string"}
                        :gender {:description "male | female | other | unknown"
                                 :type "code"
                                 :valueset {:id "fhir:administrative-genders"}}
                        :testElement {:description "Element with inlined valueset"
                                      :type "code"
                                      :valueset inlined-valueset}}}}
        profiles {:TestProfile test-profile}
        result (sut/get-inlined-valuesets {:profiles profiles, :valuesets {}})]
    (is (contains? result :valuesets) "Result should contain valuesets.")
    (is (not-empty (:valuesets result)) "Extracted valueset should not be empty.")
    (is (contains? (:valuesets result) :inlined-valueset)
        "Inlined valueset should be found among valuesets.")
    (is (= (dissoc inlined-valueset :id)
           (get-in result [:valuesets :inlined-valueset]))
        "Extracted valueset should not contain its id.")
    (is (not (contains? (:valuesets result) :fhir:administrative-genders))
        "Non-inlined valueset should not be added to valuesets.")))

(deftest test-loader
  (testing "parse-name"

    (matcho/match
     (sut/parse-name "Patient.yaml")
     {:to [:source :Patient :basic]
      :format :yaml})

    ;; (matcho/match
    ;;  (sut/parse-name "pr.Patient.example.pt1.yaml")
    ;;  {:to [:source :Patient :basic :example :pt1]
    ;;   :format :yaml})

    (matcho/match
     (sut/parse-name "Patient" "lab.yaml")
     {:to [:source :Patient :lab]
      :format :yaml})

    (matcho/match
     (sut/parse-name "vs.dic1.yaml")
     {:to [:valuesets :dic1]
      :format :yaml})

    (matcho/match
     (sut/parse-name "vs.dic1.csv")
     {:to [:valuesets :dic1 :concepts]
      :format :csv})


    )

  (def project (sut/load-project project-path))

  ;;(println (sut/build-profiles {} "resources"))

  (io/file project-path "ig.yaml")
  (io/file project-path "igpop-fhir-4.0.0")

  (comment (matcho/match
           (:base project)
           nil))

  (matcho/match
   (:source project)
   {:Patient {:basic {:elements {}}}})

  (matcho/match
   (:Patient (:profiles project))
   {:lab-report {}
    :basic {}})

  (second (get-in project [:profiles :Patient :basic :elements]))

  (get-in project [:source :Patient :basic :description])
  (get-in project [:profiles :Patient :basic :elements :gender :valueset :id])

  (keys project)

  (keys project)

  (matcho/match
   (get-in project [:valuesets :dict1])
   {:concepts [{:code "male" :display "Male"}]})

  (is (not (nil? (get-in project [:docs :pages :welcome]))))
  (is (not (nil? (get-in project [:docs :menu]))))
  (get-in project [:docs :pages])
)
