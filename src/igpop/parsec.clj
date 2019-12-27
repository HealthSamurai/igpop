(ns igpop.parsec
  (:require [clojure.string :as str]))

(defn parse-symbol [s]
  (fn [inp pos]
    (if (str/starts-with? inp s)
      {:result {:type :symbol
                :pos {:start pos :end (+ 1 pos)}
                :value (subs inp 0 1)}
       :pos (inc pos)
       :rest (subs inp 1)}
      {:error (str "Expected symbol [" s "], got [" inp "]")
       :pos pos})))

(defn pseq [& parsers]
  (fn [inp pos]
    (loop [[p & ps] parsers
           inp inp
           pos pos
           res []]
      (if (nil? p)
        res
        (let [res (p inp pos)]
          (if (:error res)
            res
            (do
              (println "* recur" (:rest res))
              (recur ps
                     (:rest res)
                     (:pos res)
                     (conj res (:result res))))))))))

(defn oneof [& parsers]
  (fn [inp pos]
    (loop [[p & ps] parsers
           errors []]
      (if (nil? p)
        {:error (str "Expected one of but none for " inp ". Or " (str/join ", " errors))
         :pos pos}
        (let [res (p inp pos)]
          (if-not (:error res)
            res
            (recur ps (conj errors (:error res)))))))))

((parse-symbol ":") "a" 0)

((parse-symbol ":") ":" 0)

((pseq (parse-symbol "1")
       (parse-symbol ":")
       (parse-symbol "2"))
 "1:2" 0)

((pseq (parse-symbol "1")
       (parse-symbol ":")
       (parse-symbol "3"))
 "1:2" 0)

((oneof (parse-symbol "1")
        (parse-symbol "2"))
 "1" 0)

((oneof (parse-symbol "1")
        (parse-symbol "2"))
 "3" 0)

((pseq (parse-symbol "1")
       (oneof (parse-symbol ":")
              (parse-symbol "+"))
       (parse-symbol "3"))
 "1+2" 0)

((pseq (parse-symbol "1")
       (oneof (parse-symbol ":")
              (parse-symbol "+"))
       (parse-symbol "3"))
 "1*2" 0)

;; (pseq (parse-symbol "{")
;;       (optional (pseq key-parser
;;                       (parse-symbol ":")
;;                       (parse-value)))
;;       (parse-symbol "}"))
