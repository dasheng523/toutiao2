(ns toutiao2.app
  (:require [toutiao2.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
