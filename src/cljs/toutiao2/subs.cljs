(ns toutiao2.subs
  (:require [re-frame.core :refer [dispatch reg-event-db reg-sub]]
            [re-frame.core :as rf]))

;;subscriptions

(defn reg-simple-sub [k]
  (reg-sub
    k
    (fn [db _]
      (k db))))

(reg-simple-sub :page)
(reg-simple-sub :docs)
(reg-simple-sub :toutiao-accounts)
(reg-simple-sub :current-account)