(ns igpop.lsp.core
  (:require
   [igpop.parser]
   [zprint.core]
   [igpop.lsp.suggest]
   [json-rpc.core :refer [proc]]))




(defmethod
  proc
  :initialize
  [ctx msg]
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


(def doc-state (atom {}))

(defn validate [ctx doc]
  (Thread/sleep 10)
  (try
    (let [ast (:ast doc)
          errors (igpop.parser/errors ast)]
      (println "ERRORS:" errors)
      (json-rpc.core/send-message ctx {:method "textDocument/publishDiagnostics"
                                       :params {:uri (get-in doc [:params :textDocument :uri])
                                                :diagnostics
                                                (->> errors
                                                     (mapv (fn [{{from :from to :to} :block msg :message}]
                                                             {:range {:start {:line (:ln from) :character (:pos from)}
                                                                      :end {:line (:ln to) :character (:pos to)}}
                                                              :message msg})))}}))
    (catch Exception err
      (println "Error in validate" err))))


(defmethod
  proc
  :textDocument/didChange
  [ctx {params :params :as msg}]
  (let [uri (get-in params [:textDocument :uri])
        newText (:text (last (:contentChanges params)))
        ast (igpop.parser/parse newText {})]
    (spit "/tmp/ast"
          (zprint.core/zprint-str ast))
    ;; (println "Change:")
    ;; (zprint.core/zprint ast)
    (reset! doc-state (assoc msg :ast ast))
    (future (validate ctx @doc-state)))
  nil)


;; completion request
;; {:jsonrpc 2.0, :id 6, :method textDocument/completion,
;;  :params {:textDocument {:uri file:///Users/mput/projects/igpop/example/src/DiagnosticReport/test.igpop},
;;           :position {:line 7, :character 0},
;;           :context {:triggerKind 2, :triggerCharacter}}}



(defmethod
  proc
  :textDocument/completion
  [ctx {{pos :position} :params :as msg}]
  (println (:method msg) pos)
  (let [ast (:ast @doc-state)
        opts (igpop.lsp.suggest/suggest ctx :Patient ast {:ln (:line pos) :pos (:character pos)})]
    {:result
     (if opts
       (->> opts
            (mapv (fn [opt]
                    {:label (name opt) 
                     :kind 14
                     :detail (name opt)})))
       [])}))

;; (defmethod
;;   proc
;;   :initialized
;;   [ctx msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :workspace/didChangeConfiguration
;;   [ctx msg]
;;   (println (:method msg) msg)
;;   {:response {}})


;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/didOpen
;;   [ctx msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/documentSymbol
;;   [ctx msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/documentColor
;;   [ctx msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/foldingRange
;;   [ctx msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/colorPresentation
;;   [ctx msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/didChange
;;   [ctx msg]
;;   (println (:method msg) msg)
;;   {:response {}})


;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/willSave
;;   [ctx msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/didSave
;;   [ctx msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :workspace/didChangeWatchedFiles
;;   [ctx msg]
;;   (println (:method msg) msg)
;;   {:response {}})

;; (defmethod
;;   json-rpc.procedure/proc
;;   :textDocument/hover
;;   [ctx msg]
;;   (println (:method msg) msg)
;;   {:response {}})


(comment

  (def ctx
    (json-rpc.core/start (atom {:type :tcp :port 7345})))

  (json-rpc.core/stop ctx)


  (zprint.core/zprint @doc-state)

  (def conn (first @(:conns (:lsp @ctx))))

  (json-rpc.tcp/send-message
   conn
   {:method "textDocument/publishDiagnostics"
    :params {:uri "file:///Users/niquola/igpop/example/test.igpop"
             :diagnostics [{:range {:start {:line 1 :character 0}
                                    :end {:line 1 :character 5}}
                            :message "Ups:()"}]}})
  (json-rpc.tcp/send-message
   conn
   {:method "textDocument/publishDiagnostics"
    :params {:uri "file:///Users/niquola/igpop/example/test.igpop"
             :diagnostics []}})

  )



