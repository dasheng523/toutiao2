(ns toutiao2.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [toutiao2.layout :refer [error-page]]
            [toutiao2.routes.home :refer [home-routes]]
            [toutiao2.zawu.routes :refer [zawu-routes zawu-service]]
            [toutiao2.routes.arikami :refer [arikami-routes]]
            [toutiao2.routes.services :refer [service-routes]]
            [toutiao2.routes.oauth :refer [oauth-routes]]
            [toutiao2.routes.websocket :refer [websocket-app]]
            [compojure.route :as route]
            [toutiao2.env :refer [defaults]]
            [mount.core :as mount]
            [toutiao2.middleware :as middleware]
            [clojure.tools.logging :as log]
            [toutiao2.config :refer [env]]))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop  ((or (:stop defaults) identity)))


(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (doseq [component (:started (mount/start))]
    (log/info component "started")))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents)
  (log/info "toutiao2 has shut down!"))

(def app-routes
  (routes
    #'websocket-app
    (-> #'home-routes
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-formats))
    (-> #'arikami-routes
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-formats))
    (-> #'zawu-routes
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-formats))
    #'oauth-routes
    #'service-routes
    #'zawu-service
    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))


(def app (middleware/wrap-base #'app-routes))
