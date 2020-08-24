(ns igpop.site.packages
  (:require [igpop.structure-definition :refer [generate-package!]])
  (:import [java.io File]))

(defn- temp-file [prefix suffix]
  (doto (File/createTempFile prefix suffix)
    .deleteOnExit))

(defn npm-package [ctx _req]
  {:status 200
   :body (generate-package! :npm ctx
                            :file (temp-file "package" ".zip"))})