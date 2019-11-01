(ns igpop.site.utils
  (:require [clojure.string :as str]))


(defn href [ctx & pth]
  (let [[pth opts] (if (map? (last pth))
                     [(butlast pth) (last pth) ]
                     [pth {}])
        fmt (when-let [fmt (:format opts)]
              (str "." fmt))]

    (if-let [bu (:base-url ctx)]
      (str (str/join "/" (into [bu] pth)) fmt)
      (str "/" (str/join "/" pth) fmt))))

(href {} "a" "b" {:format "html"})
(href {} "a" "b")

(href {:base-url "http://local.host"} "a" "b")
