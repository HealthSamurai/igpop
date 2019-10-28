(ns build
  (:require [cambada.uberjar :as uberjar]))

(defn -main [& args]
  (uberjar/-main
   "-a" "all"
   "--app-group-id" "healthsamurai"
   "--app-artifact-id" "igpop_static_site"
   "--app-version" "0.0.1"
   "-m" "clojure.main"
   "--no-copy-source"))

(comment
  (-main)

  )
