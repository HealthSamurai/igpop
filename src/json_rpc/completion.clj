(ns json-rpc.completion)


(defn get-items
  [text content uri]
  (println "!!!!!!!!!!!!!!!!!!!!!")
  (clojure.pprint/pprint text)
  (println "!!!!!!!!!!!!!!!!!!!!!")
  [{:label "i'm-completion-item"
    :kind 14
    :detail "Patient element choose"}
   {:label "another-completion"
    :detail "Some description here"}])
