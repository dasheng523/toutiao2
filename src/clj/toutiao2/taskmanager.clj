(ns toutiao2.taskmanager
  (:require
   [slingshot.slingshot :refer [throw+ try+]]
   [clojure.tools.logging :as log]
   [mount.core :refer [defstate]]
   [clojure.string :as str])
  (:import (java.util.concurrent Executors TimeUnit)))



(defstate ^:dynamic *task-pool*
  :start {:scheduer (Executors/newScheduledThreadPool 30)
          :tasks (atom {})}
  :stop (do
          (log/info "closing task pool...")
          (.shutdown (:scheduer *task-pool*))
          (reset! (:tasks *task-pool*) {})))

(defn execute-task [k dofn]
  (swap! (:tasks *task-pool*)
         assoc
         k
         (.submit (:scheduer *task-pool*)
                  dofn)))

#_(execute-task :test1 #(do (println 99) (Thread/sleep 5000) (println 10)))

(defn execute-schedule-task
  "执行定时，循环任务"
  [k dofn delays interval]
  (swap! (:tasks *task-pool*)
         assoc
         k
         (.scheduleAtFixedRate
          (:scheduer *task-pool*)
          dofn
          delays
          interval
          TimeUnit/SECONDS)))

(defn kill-task [k]
  (when-let [fut (get @(:tasks *task-pool*) k)]
    (.cancel fut true)
    (swap! (:tasks *task-pool*) dissoc k)))

#_(execute-schedule-task :test-task
                       #(println "88")
                       0
                       1)

#_(kill-task :task1)
