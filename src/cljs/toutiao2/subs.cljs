(ns toutiao2.subs
  (:require [re-frame.core :refer [dispatch reg-event-db reg-sub]]
            [re-frame.core :as rf]))

;;subscriptions

(reg-sub
  :page
  (fn [db _]
    (:page db)))

(reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(reg-sub
  :toutiao-account
  (fn [db _]
    (:toutiao-account db)))


(defn listen [k]
  @(rf/subscribe k))