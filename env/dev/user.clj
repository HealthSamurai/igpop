(ns user #_(:require [cider-nrepl.main]))

(defn -main [& args]
  #_(-> (Thread/currentThread)
      (.setName "cider"))
  #_(cider-nrepl.main/init
   ["refactor-nrepl.middleware/wrap-refactor"
    "cider.nrepl/cider-middleware"]))
