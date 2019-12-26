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


(defn start [ctx]
  (let [tp (:type @ctx)
        handler (fn [msg] (proc @ctx msg))]
    (swap! ctx assoc :handler handler)
    (if (= tp :tcp)
      (json-rpc.tcp/start ctx)
      (assert false "Not. impl"))))


(defn stop [ctx]
  (let [tp (:type @ctx)]
    (if (= tp :tcp)
      (json-rpc.tcp/stop ctx)
      (assert false "Not. impl"))))


