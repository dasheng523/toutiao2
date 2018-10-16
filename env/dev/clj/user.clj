(ns user
  (:require [luminus-migrations.core :as migrations]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [clojure.tools.namespace.repl :refer [refresh]]
            [toutiao2.config :refer [env]]
            [mount.core :as mount]
            [toutiao2.db.core]
            [toutiao2.taskmanager]
            [toutiao2.figwheel :refer [start-fw stop-fw cljs]]
            toutiao2.core))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn start []
  (mount/start-without #'toutiao2.core/repl-server))

(defn stop []
  (mount/stop-except #'toutiao2.core/repl-server))

(defn restart []
  (stop)
  (start))

(defn restart-db []
  (mount/stop #'toutiao2.db.core/*db-datasource*)
  (mount/start #'toutiao2.db.core/*db-datasource*))

(defn migrate []
  (migrations/migrate ["migrate"] (select-keys env [:database-url])))

(defn rollback []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))


