(ns igpop.lsp.core
  (:require
   [igpop.parser]
   [igpop.loader]
   [zprint.core]
   [igpop.lsp.suggest]
   [json-rpc.core :refer [proc]]
   [clojure.java.io :as io]
   [igpop.lsp.validation]
   [zprint.core :as zp]))


(defmethod
  proc
  :initialize
  [ctx msg]
  {:result
   {:capabilities {:textDocumentSync {:openClose true
                                      ;; Change notifications are sent to the server. See TextDocumentSyncKind.None, TextDocumentSyncKind.Full
                                      ;; and TextDocumentSyncKind.Incremental. If omitted it defaults to TextDocumentSyncKind.None.
                                      ;; number;
                                      ;; None = 0;
	                                    ;; Full = 1;
	                                    ;; Incremental = 2;
                                      :change 1
                                      ;; If present will save notifications are sent to the server. If omitted the notification should not be
                                      ;; sent.
                                      ;; boolean;
                                      :willSave true
                                      ;; If present will save wait until requests are sent to the server. If omitted the request should not be
                                      ;; sent.
                                      :willSaveWaitUntil false
                                      ;; If present save notifications are sent to the server. If omitted the notification should not be
                                      ;; sent.
                                      ;;SaveOptions
                                      :save  {}}
                   :hoverProvider true
                   :completionProvider {:triggerCharacters ["\n" " " ":"]}
                   :signatureHelpProvider {:triggerCharacters []}
                   ;; :definitionProvider true
                   ;; :implementationProvider true
                   ;; :referencesProvider true
                   ;; :documentHighlightProvider true
                   ;; :documentSymbolProvider true

                   ;; :documentRangeFormattingProvider true
                   ;; The server provides document formatting on typing.
                                        ;:documentOnTypeFormattingProvider DocumentOnTypeFormattingOptions;
                   ;; The server provides rename support. RenameOptions may only be
                   ;; specified if the client states that it supports
                   ;; `prepareSupport` in its initial `initialize` request.
                   ;; :renameProvider true ;;boolean | RenameOptions;
                   ;; The server provides document link support.
                   ;;:documentLinkProvider ;;DocumentLinkOptions;
                   ;; The server provides color provider support.
                   ;; :colorProvider true  ;;| ColorProviderOptions | (ColorProviderOptions & TextDocumentRegistrationOptions & StaticRegistrationOptions);
                   ;; The server provides folding provider support.
                   ;; Since 3.10.0
                   ;; :foldingRangeProvider true ;;| FoldingRangeProviderOptions | (FoldingRangeProviderOptions & TextDocumentRegistrationOptions & StaticRegistrationOptions);
                   ;; The server provides go to declaration support.
                   ;; :declarationProvider true ;; | (TextDocumentRegistrationOptions & StaticRegistrationOptions);
	                 ;; The server provides execute command support.
	                 ;; :executeCommandProvider ExecuteCommandOptions;
	                 ;; Workspace specific server capabilities

                   :workspace {
		                           ;;The server supports workspace folder.
		                           :workspaceFolders {
			                                            ;;* The server has support for workspace folders
			                                            :supported true
			                                            ;; Whether the server wants to receive workspace folder
			                                            ;; change notifications.
                                        ;
			                                            ;; If a strings is provided the string is treated as a ID
			                                            ;; under which the notification is registered on the client
			                                            ;; side. The ID can be used to unregister for these events
			                                            ;; using the `client/unregisterCapability` request.
			                                            :changeNotifications true ;;: string | boolean;
		                                              }
	                             }
                   }

    }})


(def doc-state (atom {}))







(defmethod
  proc
  :textDocument/completion
  [ctx {{pos :position :as params} :params :as msg}]
  (let [uri (get-in params [:textDocument :uri])
        doc (get-in @doc-state [:docs uri])]
    (if-let [ast (:ast doc)]
      {:result (igpop.lsp.suggest/suggest ctx msg ast)}
      {:result []})))

(defmethod
  json-rpc.core/proc
  :textDocument/hover
  [ctx {params :params meth :method :as msg}]
  (let [uri (get-in params [:textDocument :uri])
        doc (get-in @doc-state [:docs uri])]
    (if-let [ast (:ast doc)]
      {:result (igpop.lsp.suggest/hover ctx msg ast)}
      {:result []})))

(defn validate [ctx doc]
  (Thread/sleep 10)
  (try
    (let [resp (igpop.lsp.validation/validate ctx doc)]
      (json-rpc.core/send-message ctx resp))
    (catch Exception err
      (println "Error in validate" err))))

(defmethod
  proc
  :textDocument/didChange
  [ctx {params :params :as msg}]
  (let [uri (get-in params [:textDocument :uri])
        newText (:text (last (:contentChanges params)))
        ast (igpop.parser/parse newText {})
        doc (assoc msg :ast ast)]
    (swap! doc-state assoc-in [:docs uri] doc)
    (future (validate ctx doc)))
  nil)

;; (defmethod
;;   proc
;;   :initialized
;;   [ctx msg]
;;   {:result {}})

;; (defmethod
;;   json-rpc.core/proc
;;   :workspace/didChangeConfiguration
;;   [ctx msg]
;;   {:result {}})


;; (defmethod
;;   json-rpc.core/proc
;;   :textDocument/didOpen
;;   [ctx msg]
;;   {:result {}})

;; (defmethod
;;   json-rpc.core/proc
;;   :textDocument/documentSymbol
;;   [ctx msg]
;;   {:result {}})

;; (defmethod
;;   json-rpc.core/proc
;;   :textDocument/documentColor
;;   [ctx msg]
;;   {:result {}})

;; (defmethod
;;   json-rpc.core/proc
;;   :textDocument/foldingRange
;;   [ctx msg]
;;   {:result {}})

;; (defmethod
;;   json-rpc.core/proc
;;   :textDocument/colorPresentation
;;   [ctx msg]
;;   {:result {}})

;; (defmethod
;;   json-rpc.core/proc
;;   :textDocument/didChange
;;   [ctx msg]
;;   {:result {}})


;; (defmethod
;;   json-rpc.core/proc
;;   :textDocument/willSave
;;   [ctx msg]
;;   {:result {}})

;; (defmethod
;;   json-rpc.core/proc
;;   :textDocument/didSave
;;   [ctx msg]
;;   {:result {}})

;; (defmethod
;;   json-rpc.core/proc
;;   :workspace/didChangeWatchedFiles
;;   [ctx msg]
;;   {:result {}})




(comment

  (def log-file (io/writer "/tmp/igpop-lsp.log"))
  (.close log-file)

  (defn xlog [msg]
    (try
      (.write log-file msg)
      (.write log-file "\n")
      (catch Exception e
        (println e))))

  (def ctx
    (json-rpc.core/start (atom {:type :tcp
                                :port 7345
                                :json-rpc {:request (fn [_ msg]
                                                      (xlog (str "-> " (:method msg) " " (:id msg)))
                                                      (xlog (zp/zprint-str msg)))
                                           :response (fn [_ resp]
                                                       (xlog (str "<- " (:id resp)))
                                                       (xlog (zp/zprint-str resp)))
                                           :notify (fn [_ note]
                                                     (xlog (str "<! " (:method note)))
                                                     (xlog (zp/zprint-str note)))}
                                :manifest (igpop.loader/load-project "example")})))

  (json-rpc.core/stop ctx)


  

  )



