(ns json-rpc.procedure)

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

