(ns json-rpc.procedure
  (:require [json-rpc.completion]))

(def files-content (atom {}))

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

(defmethod
  proc
  :initialize
  [msg]
  (println "Init msg" msg)
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
                   ;; :hoverProvider true
                   :completionProvider {:triggerCharacters ["\n"]}
                   :signatureHelpProvider {:triggerCharacters ["."]}
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


(defmethod
  proc
  :textDocument/didChange
  [{params :params :as msg}]
  (println (:method msg) msg)
  (let [uri (get-in params [:textDocument :uri])
        newText (:text (last (:contentChanges params)))]
    (swap! files-content #(assoc % uri newText)))
  nil)


;; completion request
;; {:jsonrpc 2.0, :id 6, :method textDocument/completion,
;;  :params {:textDocument {:uri file:///Users/mput/projects/igpop/example/src/DiagnosticReport/test.igpop},
;;           :position {:line 7, :character 0},
;;           :context {:triggerKind 2, :triggerCharacter}}}

(defmethod
  proc
  :textDocument/completion
  [{params :params :as msg}]
  (println (:method msg) msg)
  (let [uri (get-in params [:textDocument :uri])
        position (:position params)
        content (get @files-content uri)
        items (json-rpc.completion/get-items content position uri)]
    {:result items}))

;; (defmethod
;;   proc
;;   :initialized
;;   [msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :workspace/didChangeConfiguration
;;   [msg]
;;   (println (:method msg) msg)
;;   {:response {}})


;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/didOpen
;;   [msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/documentSymbol
;;   [msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/documentColor
;;   [msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/foldingRange
;;   [msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/colorPresentation
;;   [msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/didChange
;;   [msg]
;;   (println (:method msg) msg)
;;   {:response {}})


;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/willSave
;;   [msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/didSave
;;   [msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :workspace/didChangeWatchedFiles
;;   [msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/hover
;;   [msg]
;;   (println (:method msg) msg)
;;   {:response {}})
