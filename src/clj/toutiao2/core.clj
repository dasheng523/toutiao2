(ns toutiao2.core
  (:require [toutiao2.handler :as handler]
            [luminus.repl-server :as repl]
            [luminus.http-server :as http]
            [luminus-migrations.core :as migrations]
            [toutiao2.config :refer [env]]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [mount.core :as mount])
  (:gen-class))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(mount/defstate ^{:on-reload :noop}
  http-server
  :start
  (http/start
   (-> env
       (assoc :handler handler/app)
       (update :io-threads #(or % (* 2 (.availableProcessors (Runtime/getRuntime)))))
       (update :port #(or (-> env :options :port) %))))
  :stop
  (http/stop http-server))

(mount/defstate ^{:on-reload :noop}
  repl-server
  :start
  (when-let [nrepl-port (env :nrepl-port)]
    (repl/start {:port nrepl-port :handler cider-nrepl-handler}))
  :stop
  (when repl-server
    (repl/stop repl-server)))


(defn init-jndi []
  (System/setProperty "java.naming.factory.initial"
                      "org.apache.naming.java.javaURLContextFactory")
  (System/setProperty "java.naming.factory.url.pkgs"
                      "org.apache.naming"))

(defn start-app [args]
  (init-jndi)
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. handler/destroy)))

(defn -main [& args]
  (cond
    (some #{"init"} args)
    (do
      (mount/start #'toutiao2.config/env)
      (migrations/init (select-keys env [:database-url :init-script]))
      (System/exit 0))
    (some #{"migrate" "rollback"} args)
    (do
      (mount/start #'toutiao2.config/env)
      (migrations/migrate args (select-keys env [:database-url]))
      (System/exit 0))
    :else
    (start-app args))
  )
