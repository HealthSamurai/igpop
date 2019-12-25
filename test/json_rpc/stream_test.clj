(ns json-rpc.stream-test
  (:require [json-rpc.stream :as sut]
            [cheshire.core]
            [clojure.test :refer :all]))

(deftest test-json-rpc-stream

  (def msg
    (let [msg (cheshire.core/generate-string {:method "init" :id 0})]
      (str (format "Content-Length: %s\r\n\r\n" (count (.getBytes msg))) msg)))

  (sut/read-string-untill :buf [\return \newline \return \newline])



  )



