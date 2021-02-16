(ns igpop.site.utils
  (:require [clojure.string :as str]
            [igpop.structure-definition :refer [npm-manifest]]))

(defn href [ctx & pth]
  (let [[pth opts] (if (map? (last pth))
                     [(butlast pth) (last pth) ]
                     [pth {}])
        fmt (when-let [fmt (:format opts)]
              (str "." fmt))
        res (if-let [bu (:base-url ctx)]
              (str (str/join "/" (into [bu] pth)) fmt)
              (str "/" (str/join "/" pth) fmt))]
    res))


(defn to-local-href [ctx url]
  (str/replace url (:url ctx) ""))

(defn p [v k] (prn v) v)

(defn generate-docs-href [ctx]
  (let [docs (:pages (:docs ctx))
        doc-folder (:folder
                    (first
                     (sort-by :title
                      (->>
                       docs
                       (map (fn [[doc-id doc]]
                               {:title (keyword (or (:title (:basic doc)) (name doc-id)))
                                :folder doc-id}))))))
        basic-exists? (if (some? doc-folder)
                        (if (:basic (doc-folder docs)) :basic))
        doc-instance (if (some? doc-folder)
                       (or basic-exists?
                           (first (keys (into (sorted-map) (doc-folder docs))))))
        doc-link (if (and doc-folder doc-instance)
                   (href ctx  "docs" (name doc-folder) (str (name doc-instance) ".html"))
                   (href ctx "docs"))]
    doc-link))

(defn generate-profiles-href [ctx]
  (let [profiles (:source ctx)
        profile-folder (first (keys (into (sorted-map) profiles)))
        basic-exists? (if (some? profile-folder)
                        (if (:basic (profile-folder profiles)) :basic))
        doc-instance (if (some? profile-folder)
                       (or basic-exists?
                           (first (keys (into (sorted-map) (profile-folder profiles))))))
        profile-link (if (and profile-folder doc-instance)
                   (href ctx  "profiles" (name profile-folder) (str (name doc-instance) ".html"))
                   (href ctx "profiles"))]
    profile-link))

(defn generate-valueset-href [ctx]
  (let [valuesets (:valuesets ctx)
        valueset-instance (first (keys (sort-by first valuesets)))
        valueset-link (if valueset-instance
                        (href ctx "valuesets" (str (name valueset-instance) ".html"))
                        (href ctx "valuesets"))]
    valueset-link))

(defn generate-codesystem-href [ctx]
  (let [codesystems (:codesystems ctx)
        codesystem-instance (first (keys (sort-by first codesystems)))
        codesystem-link (if codesystem-instance
                        (href ctx "codesystems" (str (name codesystem-instance) ".html"))
                        (href ctx "codesystems"))]
    codesystem-link))

(defn generate-package-href [ctx]
  (let [manifest (npm-manifest ctx)]
    (href ctx (:name manifest) {:format "zip"})))

(defn generate-fhir-package-href [ctx]
  (href ctx "package" {:format "tgz"}))

(defn deep-merge
  [& maps]
  (letfn [(m [& xs]
            (if (some #(and (map? %) (not (record? %))) xs)
              (apply merge-with m xs)
              (last xs)))]
    (reduce m maps)))

(defn dissoc-in [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))
