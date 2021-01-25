(ns igpop.fhir-package
  "FHIR Package creation utility.
   What is FHIR Package - http://registry.fhir.org/learn
   Creationg Spec - https://confluence.hl7.org/display/FHIR/NPM+Package+Specification "
  (:require [clojure.java.io :as io]
            [flatland.ordered.map :refer [ordered-map]]
            [cheshire.core :as json]
            [igpop.structure-definition :as sd])
  (:import (org.apache.commons.compress.archivers.tar TarArchiveEntry TarArchiveOutputStream)
           (org.apache.commons.compress.compressors.gzip GzipCompressorOutputStream)))


(defn ->package-json
  "creates `package.json` content from `ctx`"
  [{:keys [id version title url
           description fhir keywords author
           maintainers licence] :as ctx}]
  (ordered-map
   :name         (or id ""),
   :version      (or version "")
   :canonical    (or url ""),
   :url          (or url ""),
   :title        (or title ""),
   :description  (or description ""),
   :fhirVersions [(or fhir "")],
   :dependencies {"hl7.fhir.core" (or fhir "")},
   :keywords     (or keywords []),
   :author       (or author ""),
   :maintainers  (or maintainers []),
   :license      (or licence "CC0-1.0")))


(defn ->index-json
  "creates `.index.json` content from structure-definitions"
  [struct-defs]
  ;; TODO make something according spec of index-version
  ;; //:index-version a fixed number that identifies the version of this file; tools should rebuild the .index.json file if they encounter an unrecognised number
  {:index-version 1
   :files
   (for [{:keys [id resourceType url version kind type]} struct-defs]
     (cond-> {:filename     (str id ".json")
              :resourceType resourceType
              :id           id
              :url          url}
       version (assoc :version version) ;; the business version, if the resource has one (e.g. a property "version" which is a primitive)
       kind    (assoc :kind kind)       ;; the value of a the "kind" property in the resource, if it has one and it's a primitive
       type    (assoc :type type)       ;; the value of a the "type" property in the resource, if it has one and it's a primitive
       ))})


;; Format
;; A FHIR package is a tarball (tar in gzip). The package contains
;;     a subfolder named 'package'
;;     a package manifest (package/package.json)
;;     an index file (package/.index.json)
;;     A set of resource files, also in the package subfolder
;;     It MAY contain additional content, like example resources or documentation:
;;         such files SHALL not be in the package subfolder
;;         Example resources SHOULD be placed in an /examples folder.
;;         Tools working with examples, SHALL understand this. Note that the interpretation of what is an example resource can be unclear in some cases
;;         this may include XML schemas in an "xml" subfolder
;;         this may include openAPI files in an "openapi" subfolder
;;         this may include turtle RDF representations in an rdf folder
;;         Package consumers SHALL ignore content in other subfolders that they do not use (and most consumers will only use the resources in /package)
;;
;; Tarballs SHALL be in the original tarball format (e.g. a 99 character file name length limit).Te examples

(defn make-package-tgz
  "Create FHIR Package file
  Creates tgz file in `/[project-home]/build/[project-id].tgz`
  You can override output file by specifying `:file` proprty in opts"
  [ig-ctx & {:as opts}]
  (let [file (or (:file opts)
                 (io/file (:home ig-ctx) "build" (str (:id ig-ctx) ".tgz")))
        resources (sd/project->structure-definitions ig-ctx)]
    (io/make-parents file)
    (with-open [fout  (io/output-stream file)
                gzout (GzipCompressorOutputStream. fout)
                tout  (TarArchiveOutputStream. gzout)]
      (letfn [(add-tgz-entry [entry-name content]
                (let [entry (TarArchiveEntry. entry-name)
                      bytes (.getBytes (json/generate-string content {:pretty true}) "UTF-8")]
                  (.setSize entry (count bytes))
                  (.putArchiveEntry tout entry)
                  (io/copy bytes tout)
                  (.closeArchiveEntry tout)))]
        (doseq [sd resources]
          (add-tgz-entry (str "package/" (:id sd) ".json") sd))
        (add-tgz-entry "package/package.json" (->package-json ig-ctx))
        (add-tgz-entry "package/.index.json" (->index-json resources)))
      (.finish tout))))


(comment

  (def hm (.getAbsolutePath (io/file  "../ig-ae")))

  (require '[igpop.loader])

  (def ctx (igpop.loader/load-project hm))

  (keys ctx)
  (-> (sd/project->bundle ctx)
      :entry first :resource :id)

  (make-package-tgz ctx)

  nil)

