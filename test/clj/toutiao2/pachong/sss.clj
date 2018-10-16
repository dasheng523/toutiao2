(ns toutiao2.pachong.sss
  (:require
   [slingshot.slingshot :refer [throw+ try+]]
   [spider.spiteml :as teml]
   [spider.logic :as logic]
   [clojure.tools.logging :as log]
   [net.cgrand.enlive-html :as enlive]
   [mount.core :refer [defstate]])
  (:import (java.util.concurrent Executors TimeUnit)))

;; 线程控制器，自动创建运行计划，任务到点就执行。
;; 用惰性序列模拟运行计划，有问题是如何将新任务插入序列
;; 做合并序列的功能，然后遍历这个序列，取最新的序列情况。
;; 序列如何表示呢？用无穷序列。

(defn create-simple-queue [dofn sleep]
  (concat [dofn]
          (repeat sleep nil)
          (lazy-seq (create-simple-queue dofn sleep))))


(defn merge-queue [que1 que2]
  (map #(if (and %1 %2) (flatten [%1 %2]) (or %1 %2))
       que1 que2))

(defn remove-task [que task]
  (let [node (first que)]
    (cond
      (= node task)
      (cons nil (lazy-seq (remove-task (rest que) task)))
      (and
       (coll? node)
       (some #(= task %) node))
      (cons (remove #(= task %) node) (lazy-seq (remove-task (rest que) task)))
      :else (cons node (lazy-seq (remove-task (rest que) task))))))


(def exequeue (ref (create-simple-queue nil 1)))
(def tmanager-state (atom true))

(defn run-fun
  "传入函数和间隔秒数，将每隔n秒执行一次dofn。"
  [dofn n]
  (dosync
   (alter exequeue merge-queue (create-simple-queue dofn n)))
  nil)

(defn reset-queue []
  (dosync (alter exequeue #(identity %2) (create-simple-queue nil 1)))
  nil)


(defn tmanager-stop []
  (reset! tmanager-state false))

(defn start-queue []
  (let [queue @exequeue
        firnode (first queue)]
    (when @tmanager-state
      (Thread/sleep 1000)
      (if firnode
        (cond (coll? firnode)
              (doseq [dofn firnode]
                (future (dofn)))
              (fn? firnode)
              (future (firnode))))
      (reset! exequeue (rest @exequeue))
      (recur))))

(defn start-task [filename]
  (let [{:keys [task_name task_interval] :as temdata}
        (teml/load-template filename)
        dofn #(logic/grap-data temdata)]
    (swap! exequeue merge-queue
           (create-simple-queue dofn task_interval))))

(defn tmanager-start []
  (reset! tmanager-state true)
  (future (start-queue)))

#_(reset-queue)

#_(run-fun #(println 2) 3)

#_(take 10 @exequeue)

#_(let [{:keys [task_name task_interval] :as temdata}
        (teml/load-template "1.json")
        dofn #(logic/grap-data temdata)]
    (run-fun dofn 60))




#_(defstate ^:dynamic *tmanager*
  :start (tmanager-start)
  :stop (tmanager-stop))



#_(start-task "1.json")

#_(merge-queue (create-simple-queue afn 1)
             (create-simple-queue bfn 5))


