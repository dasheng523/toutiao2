(ns toutiao2.shop.cache
  (:require [clojure.core.cache :as cache]))

(def default-expired (* 2 3600 1000)) ;默认过期时间 2小时

;; 过期时间是2小时
(def default-cache-store
  (atom (cache/ttl-cache-factory
         {}
         :ttl default-expired)))

(defn get-cache [k]
  (cache/lookup (swap! default-cache-store
                       #(if (cache/has? % k)
                          (cache/hit % k)
                          (cache/miss % k nil)))
                k))

(defn set-cache! [k v]
  (swap! default-cache-store
         #(cache/miss % k v)))
