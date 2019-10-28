(ns profile-site.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.handler.dump :refer [handle-dump]]
            [compojure.route :as route]
            [compojure.core :refer :all]
            [garden.core :as gc]
            [hiccup.core :as hc]
            [profile-site.views :as psv]
            [profile-site.style :as pss]
            [profile-site.utils :refer :all]
            [profile-site.data :as psd])
  (:gen-class))

(defn patient-page [request]
  {:status 200
   :headers {"Content-type" "text/html"}
   :body (psv/profile-page->html psd/patient-profile)})

(defn organization-page [request]
  {:status 200
   :headers {"Content-type" "text/html"}
   :body (psv/profile-page->html psd/organization-profile)})

(defn home-page [request]
  {:status 200
   :headers {"Content-type" "text/html"}
   :body (psv/home-page->html)})

(defroutes app
  (GET "/" [] #'home-page)
  (GET "/profiles/Patient" [] #'patient-page)
  (GET "/profiles/Organization" [] #'organization-page)
  (route/resources "/assets/")
  (route/not-found "This page doesn't exist"))

(defn -main []
  (jetty/run-jetty (wrap-reload app) {:port 3000}))
