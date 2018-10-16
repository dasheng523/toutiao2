(ns toutiao2.api.cache
  (:require [toutiao2.utils :as utils]))

(defn make-cache []
  (atom {}))
(defn set-cache [cache k v expires]
  (swap! cache assoc k
         {:v v
          :set-time (utils/current-time)
          :expires expires}))

(defn get-cache [cache k]
  (when-let [{:keys [v set-time expires]} (get @cache k)]
    (if (> (- (utils/current-time) set-time) expires)
      nil
      v)))

(defn clear-cache
  ([cache]
   (reset! cache {}))
  ([cache k]
   (swap! cache dissoc k)))
