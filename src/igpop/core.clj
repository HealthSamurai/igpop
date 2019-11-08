(ns igpop.core
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [igpop.site.core :as site]
            [clojure.set])
  (:gen-class))

(defmulti run (fn [nm & args] (keyword nm)))

(def commands
  {"help"     {:fn :help}
   "validate" {:fn :validate}
   "build"    {:fn :build}
   "dev"      {:fn :dev}})

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
  (println "Build..." args)
  (site/build (System/getProperty "user.dir") (second args)))

(defmethod run
  :dev
  [& args]
  (println "Dev..." args)
  (site/start (System/getProperty "user.dir") 8899))

(defn -main [& args]
  (if-let [cmd (first args)]
    (if-let [handler (get commands cmd)]
      (apply run args)
      (do
        (println "No such command - " cmd)
        (run :help)))
    (run :help)))

(comment

  (System/getProperty "user.dir")

  (-main "dev" "dir")


  )
