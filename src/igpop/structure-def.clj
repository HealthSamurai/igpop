(ns igpop.structure-def)
  ;;(:require [clojure.instant :as tm]))

;;(def last-updated (tm/read-instant-date (str (java.util.Date. (System/currentTimeMillis)))))

(defn name-that-profile [rt prn] (str (name rt) (when (not (= "basic" (name prn)))
                                                           (str "_" (name prn)))))

(defn generate-structure [{profiles :profiles :as ctx}]
  (let [m {:resourceType "Bundle"
           :id "resources"
           :meta {:lastUpdated (java.util.Date.)}
           :type "collection"}]
    (assoc m :entry
           (into [] (apply concat (for [[rt prls] profiles]
                                    (for [[prn props] prls]
                                      (assoc {} :fullUrl (str "baseUrl" "/" (name-that-profile rt prn))))))))))
