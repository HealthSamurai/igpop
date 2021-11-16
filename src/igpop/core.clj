(ns igpop.core
  (:require [clojure.java.io :as io]
            [clojure.set]
            [zen.core :as zen]))

(defmulti run (fn [cmd & _]))

(defmethod run 'igpop/dev-server
  [_ & [dir]]

  )


(defn -main [& args]

  )

(comment

  (System/getProperty "user.dir")

  (-main "dev" "dir")

  (site/start "/Users/niquola/fhir-ru" 8899)

  (site/build "/Users/niquola/fhir-ru" "http://fhir.ru")

  )
