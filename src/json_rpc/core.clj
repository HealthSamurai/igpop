(ns json-rpc.core
  (:require [json-rpc.tcp]))

(defmulti proc (fn [ctx {meth :method :as arg}] (keyword meth)))

(defmethod proc
  :default
  [ctx msg]
  (println "Not impl." msg)
  {})

(defmethod proc
  :json-rpc/ping
  [ctx msg]
  {:id (:id msg) :message "pong"})

(defn send-message [ctx msg]
  (json-rpc.tcp/send-message (:channel ctx) msg))

(defn start [ctx]
  (let [tp (:type @ctx)
        handler (fn [conn msg] (proc (assoc @ctx :channel conn) msg))]
    (swap! ctx assoc :handler handler)
    (if (= tp :tcp)
      (json-rpc.tcp/start ctx)
      (assert false "Not. impl"))))


(defn stop [ctx]
  (let [tp (:type @ctx)]
    (if (= tp :tcp)
      (json-rpc.tcp/stop ctx)
      (assert false "Not. impl"))))


