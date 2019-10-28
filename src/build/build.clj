(ns build
  (:require [cambada.uberjar :as uberjar]))


(defn -main [& args]
  (uberjar/-main
   "-a" "all"
   ;; "-p" "../ui/build"
   "--app-group-id" "healthsamurai"
   "--app-artifact-id" "igpop"
   "--app-version" "0.0.1"
   "-m" "clojure.main"
   "--no-copy-source"))

(comment
  (-main))


