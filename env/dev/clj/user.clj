
(ns user
  (:require [luminus-migrations.core :as migrations]
            [clojure.tools.namespace.repl :refer [refresh]]
            [toutiao2.config :refer [env]]
            [mount.core :as mount]
            [toutiao2.figwheel :refer [start-fw stop-fw cljs]]
            toutiao2.core))

(defn start []
  (mount/start-without #'toutiao2.core/repl-server))

(defn stop []
  (mount/stop-except #'toutiao2.core/repl-server))

(defn restart []
  (stop)
  (start))

(defn migrate []
  (migrations/migrate ["migrate"] (select-keys env [:database-url])))

(defn rollback []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))


