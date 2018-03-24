(ns toutiao2.shop.cache
  (:require [clojure.core.cache :as cache]))

(def default-expired (* 2 3600 1000)) ;默认过期时间 2小时

(defn create-cache
  "创建一个普通的缓存"
  ([expired]
   (atom (cache/ttl-cache-factory
          {}
          :ttl expired)))
  ([]
   (create-cache default-expired)))


(defn set-cache!
  [c k v]
  (swap! c #(cache/miss % k v)))


(defn get-cache!
  [c k]
  (cache/lookup
   (swap! c
          #(if (cache/has? % k)
             (cache/hit % k)
             (cache/miss % k nil)))
   k))

(defn get-cache-live!
  "获取缓存，并且使得缓存寿命续期"
  [c k]
  (let [data (if (cache/has? @c k)
               (get @c k)
               nil)]
    (set-cache! c k data)
    data))

; 一些正在用的缓存
(def user-token-cache (create-cache))
(def token-cache (create-cache))

(defn get-token-data [token k]
  (get-cache-live! token-cache (str (name k) token)))
