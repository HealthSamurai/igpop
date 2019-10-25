(ns build
  (:require [cambada.uberjar :as uberjar]))


(defn -main [& args]
  (uberjar/-main
   "-a" "all"
   ;; "-p" "../ui/build"
   "--app-group-id" "healthsamurai"
   "--app-artifact-id" "igpop"
   "--app-version" "0.0.1"
   "-m" "clojure.main"
   "--no-copy-source"))

(comment
  (-main)

  )


;; (defn default-options [] 
;;   (cli/args->task [] jar/cli-options))

;; ;; (def opts
;; ;;  (cli/args->task
  
;; ;;   cli-options))



;; (def build-config
;;   {:defaults {:aot ['all]
;;               :merge-config true
;;               :out "target"
;;               :main "clojure.main"
;;               :app-group-id "health-samurai"
;;               :app-version "1.0.0"} 
;;    ;; app-artifact-id
;;    :builds {:libox       {:extra-paths ["../ui/build/cluster"]}
;;             ;; :aidbox      {:extra-paths ["../ui/build/aidbox"]}
;;             ;; :cluster     {:extra-paths ["../ui/build/cluster"]}
;;             ;; :aidboxone   {:copy-source false
;;             ;;               :extra-paths ["../ui/build/cluster"]}
;;             ;; :devbox      {:copy-source false
;;             ;;               :extra-paths ["../ui/build/cluster"]}
;;             }})

;; (defn mk-options [cfg]
;;   (let [opts (default-options)]
;;     (-> 
;;      (mapv (fn [[id b]]
;;              (merge
;;               opts
;;               (:defaults cfg)
;;               (assoc b :app-artifact-id (name id))))
;;            (:builds cfg)))))

;; ;; (spit "/tmp/opts.yaml"
;; ;;       (clj-yaml.core/generate-string opts))

;; (defn multi-build [& args]
;;   (doseq [b (mk-options build-config)]
;;     (cli/info "Build: " (:app-artifact-id b))
;;     (uberjar/apply! b)))

;; (comment

;;   (multi-build)


;;   )
