(ns json-rpc.tcp-test
  (:require [json-rpc.tcp :as sut]
            [clojure.test :refer :all])
  (:import

   [java.nio.charset StandardCharsets]
   [java.nio ByteBuffer]))

(defn str-buf [s]
  (ByteBuffer/wrap (.getBytes s)))

(deftest tcp-test

  (testing "decoder"

    (def state (atom []))
    (def decode (sut/decoder (fn [msg] (swap! state conj  msg))))

    (def msg "Content-Length: 24\r\n\r\n{\"method\":\"init\",\"id\":0}")


    (sut/parse-header "Content-Length:10\r\n\r\n")

    (decode (str-buf "Content-Length: 24\r\n\r\n{\"method\":\"init\",\"id\":0}"))

    (is (= @state
           [{:id 0 :method "init"}]))

    (decode
     (str-buf
      (str "Content-Length: 24\r\n\r\n{\"method\":\"init\",\"id\":1}"
           "Content-Length: 24\r\n\r\n{\"method\":\"init\",\"id\":2}")))

    (is (= @state
           [{:id 0 :method "init"}
            {:id 1 :method "init"}
            {:id 2 :method "init"}]))

    (decode (str-buf "Content-Length: 24\r\n\r\n{\"met"))
    (decode (str-buf "hod\":\"init\",\"id\":3}"))

    (is (= @state
           [{:id 0 :method "init"}
            {:id 1 :method "init"}
            {:id 2 :method "init"}
            {:id 3 :method "init"}]))

    )







  )

