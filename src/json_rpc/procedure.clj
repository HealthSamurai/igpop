(ns json-rpc.procedure)

(defmulti proc (fn [{meth :method :as arg}] (keyword meth)))

(defmethod proc
  :default
  [msg]
  (println "Not impl." msg)
  {})

(defmethod proc
  :ping
  [msg]
  {:id (:id msg) :message "pong"})

(proc {:method "default"})
(proc {:id 1 :method "ping"})
