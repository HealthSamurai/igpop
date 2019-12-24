(ns json-rpc.core
  (:require [promesa.core :as p])
  (:import
   [java.nio.channels
    AsynchronousServerSocketChannel
    AsynchronousSocketChannel
    SocketChannel
    CompletionHandler
    AsynchronousCloseException]
   [java.net InetSocketAddress]
   [java.nio ByteBuffer]))

(set! *warn-on-reflection* true)

(defn client [^String host ^Integer port]
  (let [cl-addr (InetSocketAddress. host port)
        cl (SocketChannel/open cl-addr)]
    cl))

(defn client-send [^SocketChannel cl data]
  (let [^ByteBuffer buf (ByteBuffer/wrap data)]
    (try (.write cl buf)
         (catch Exception e
           (.close cl)))))

(defn read-channel [^AsynchronousSocketChannel channel size enqueue conns]
  (let [buf (ByteBuffer/allocateDirect size)]
    (println "read channel")
    (.read channel buf nil
           (reify CompletionHandler
             (completed [this cnt _]
               (println "Completed" cnt)
               (when (= -1 cnt)
                 (println "Disconnected " channel)
                 (swap! conns disj channel))
               (when (> cnt 0)
                 (let [bytes (byte-array cnt)]
                   (.flip buf)
                   (.get buf bytes)
                   (enqueue bytes)
                   (.clear buf))
                 (.read channel buf nil this)))
             (failed [this e _]
               (if (instance? AsynchronousCloseException e)
                 (println "Closed " channel)
                 (do (.close channel)
                     (println "! Failed (read):" e))))))))

(defn handler [listener enqueue conns]
  (reify CompletionHandler
    (failed [this e _]
      (if (instance? AsynchronousCloseException e)
        (println "Closed..")
        (println "! Failed (read):" e)))
    (completed [this sc _]
      (println "Incomming connection " sc)
      (swap! conns conj sc)
      (.accept ^AsynchronousServerSocketChannel listener nil  this)
      (read-channel sc 1000000 enqueue conns))))

(defn start [ctx]
  (let [assc (AsynchronousServerSocketChannel/open)
        port 7345
        sa  (InetSocketAddress. port)
        listener (.bind assc sa)
        enqueue (or (:enqueue @ctx) println)
        conns (atom #{})]
    (println "tcp logs server started at  " port)
    (.accept listener nil (handler listener enqueue conns))
    (swap! ctx (fn [ctx] (update ctx :zmq assoc :sock assc :conns conns))))
  ctx)


(defn stop [ctx]
  (when-let [conns (get-in @ctx [:zmq :conns])]
    (doseq [c @conns]
      (.close ^AsynchronousSocketChannel c))
    (reset! conns #{}))

  (when-let [sock (get-in @ctx [:zmq :sock])]
    (println "Stop server")
    (.close ^AsynchronousSocketChannel sock)
    (println "ok")))

(comment
  (stop ctx)

  (def ctx (start (atom {:zmq {:port 7345}
                         :enqueue (fn [msg] (println "Incomming " (String. msg)))})))

  ctx

  (def cl (client "localhost" 7777))

  (.close cl)

  (.isOpen cl)
  (.isConnected cl)

  (client-send cl (for [i (range 100)] {:a i}))


  )


