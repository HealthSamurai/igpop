(ns igpop.site.packages
  (:require [igpop.structure-definition :refer [generate-package!]]
            [igpop.fhir-package :refer [generate-fhir-package!]])
  (:import [java.io File]))

(defn- temp-file [prefix suffix]
  (doto (File/createTempFile prefix suffix)
    .deleteOnExit))

(defn npm-package [ctx _req]
  {:status 200
   :body (generate-package! :npm ctx
                            :file (temp-file "package" ".zip"))})

(defn fhir-package [ctx _req]
  {:status 200
   :body (generate-fhir-package! ctx :file (temp-file "package" ".tgz"))})
