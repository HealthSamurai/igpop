(ns igpop.schema-test
  (:require [igpop.schema :as sut]
            [clojure.test :refer :all]
            [matcho.core :as matcho]
            [clojure.java.io :as io]
            [igpop.loader :as loader]
            [cheshire.core :refer :all]))

(deftest test-schema-gen

  (def project-path (.getPath (io/resource "test-project")))

  (def project (loader/load-project project-path))

  (comment
    (sut/generate-schema project)

    (get-in project [:profiles :Patient :basic :elements :name :elements :given])

    (spit (io/file "../test-schema.json") (generate-string (sut/generate-schema project) {:pretty true}))

    (def simple-types (mapv #(keyword %)
                            (clojure.string/split "base64Binary boolean canonical code date dateTime decimal id instant integer markdown oid positiveInt string time unsignedInt uri url uuid" #" ")))

    (def complex-types (mapv #(keyword %) (clojure.string/split "Address Age Annotation Attachment CodeableConcept Coding ContactPoint Count Distance Duration HumanName Identifier Money Period Quantity Range Ratio Reference SampledData Signature Timing ContactDetail Contributor DataRequirement Expression ParameterDefinition RelatedArtifact TriggerDefinition UsageContext Dosage Meta Reference Extension Narrative" #" ")))

    (def simple-types-definitions (assoc {} :definitions (let [types (-> "/home/victor/Documents/Diploma/JSON schema validation/lib/fhir-schemas/fhir.schema.json"
                                                                         io/file
                                                                         slurp
                                                                         (parse-string true)
                                                                         (get-in [:definitions]))]
                                                           (select-keys types (for [[k v] types :when (some #(= k %) simple-types)] k)))))

    (def complex-types-definitions (assoc {} :definitions (let [types (-> "/home/victor/Documents/Diploma/JSON schema validation/lib/fhir-schemas/fhir.schema.json"
                                                                         io/file
                                                                         slurp
                                                                         (parse-string true)
                                                                         (get-in [:definitions]))]
                                                           (select-keys types (for [[k v] types :when (some #(= k %) complex-types)] k)))))

    (spit (io/file "/home/victor/Documents/Trash/definitions(simple).json") (generate-string simple-types-definitions {:pretty true}))

    (spit (io/file "/home/victor/Documents/Trash/definitions(complex).json") (generate-string complex-types-definitions {:pretty true}))

    (get-in project [:profiles :AllergyIntolerance :basic :elements])

    (sut/get-concepts project (get-in project [:profiles :AllergyIntolerance :basic :elements]))
    )

  (testing "get concepts"
    (matcho/match
     ["active" "inactive" "resolved"] (sut/get-concepts project (get-in project [:profiles :AllergyIntolerance :basic :elements :clinicalStatus])))

    (matcho/match
     ["male" "female" "androgin" "unknonw"] (sut/get-concepts project (get-in project [:profiles :Patient :basic :elements :gender])))

    ))
