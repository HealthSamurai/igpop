(ns igpop.core
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.set])
  (:gen-class))

(defmulti run (fn [nm & args] (keyword nm)))

(def commands
  {"help"     {:fn :help}
   "validate" {:fn :validate}
   "build"    {:fn :build}})

(defmethod run
  :help
  [_ & args]
  (println "igpop [cmd] [subcmd] opts")
  (doseq [[k v] commands]
    (println " " k " - " (:desc v))))

(defmethod run
  :validate
  [& args]
  (println "Validate..." args))

(defmethod run
  :build
  [& args]
  (println "Build..." args))

(defn -main [& args]
  (if-let [cmd (first args)]
    (do
      (println "Run " cmd)
      (if-let [handler (get commands cmd)]
        (do
          (println ">>" handler)
          (apply run args))
        (run :help)))
    (run :help)))
