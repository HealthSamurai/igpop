(ns build
  (:require [cambada.uberjar :as uberjar]))

(defn -main [& args]
  (let [dir (str (System/getProperty "user.dir") "/resources" "/public")]
    (spit (clojure.java.io/file dir "static-resources") (clojure.string/join " " (for [f (->> dir
                                                                                              clojure.java.io/file
                                                                                              file-seq
                                                                                              (filter #(not (.isDirectory %))))]
                                                                                   (.getName f)))))

  (uberjar/-main
   "-a" "all"
   "-p" "resources"
   "--app-group-id" "healthsamurai"
   "--app-artifact-id" "igpop"
   "--app-version" "0.0.1"
   "-m" "igpop.core"
   "--no-copy-source"))

(comment
  (-main))
