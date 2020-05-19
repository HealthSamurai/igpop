(ns igpop.core
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [igpop.site.core :as site]
            [clojure.set]
            [igpop.lsp.core :as lsp])
  (:gen-class))

(defmulti run (fn [nm & args] (keyword nm)))

(def commands
  {"help"     {:fn :help
               :desc "very concise usage guide"}
   "validate" {:fn :validate
               :desc "todo"}
   "build"    {:fn :build
               :desc "USAGE: igpop build {your_baseurl} EXAMPLE: igpop build /igpop"}
   "dev"      {:fn :dev
               :desc "-p to setup a port (default is 8899)"}
   "lsp"      {:fn :lsp
               :desc "-p to setup a port (default is 7345)"}})

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
  (let [i (.indexOf args "-p")
        port (last args)]
    (cond
      (and (> i 0) (not (= "-p" port)))
      (site/start (System/getProperty "user.dir") (Integer. port))
      (and (< i 0) (= "dev" port))
      (site/start (System/getProperty "user.dir") 8899)
      :else
      (run :help))))

(defmethod run
  :lsp
  [& args]
  (println "LSP..." args)
  (let [i (.indexOf args "-p")
        port (last args)]
    (cond
      (and (> i 0) (not (= "-p" port))) (lsp/start (System/getProperty "user.dir") (Integer. port))
      (and (< i 0) (= "lsp" port)) (lsp/start (System/getProperty "user.dir") 7345)
      :else
      (run :help))))

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
