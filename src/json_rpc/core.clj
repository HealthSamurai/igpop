(ns json-rpc.core
  (:require
   [org.httpkit.server :as http]
   [json-rpc.tcp])
  (:import org.httpkit.server.AsyncChannel))

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
  (let [chann (:channel ctx)]
    (when-let [on-notif (get-in ctx [:json-rpc :notify])]
      (on-notif ctx msg))
    (if (instance? org.httpkit.server.AsyncChannel chann)
      (http/send! chann (cheshire.core/generate-string msg))
      (json-rpc.tcp/send-message chann msg))))

(defn start
  ([ctx] (start ctx nil))
  ([ctx group] (let [tp (:type @ctx)
            {on-req :request on-resp :response} (:json-rpc @ctx)
            handler (fn [conn msg]
                      (when on-req (on-req @ctx msg))
                      (let [resp (proc (assoc @ctx :channel conn) msg)]
                        (when on-resp (on-resp @ctx resp))
                        resp))]
        (swap! ctx assoc :handler handler)
        (if (= tp :tcp)
          (json-rpc.tcp/start ctx group)
          (assert false "Not. impl")))))


(defn stop [ctx]
  (let [tp (:type @ctx)]
    (if (= tp :tcp)
      (json-rpc.tcp/stop ctx)
      (assert false "Not. impl"))))
