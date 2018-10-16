(ns toutiao2.shop.bll.UserContext
  (:require [clojure.core.cache :as cache]))

; 上下文载体，每个用户信息缓存两个小时，如果读取或者写入，都会续时间
(def context (atom (cache/ttl-cache-factory {} :ttl (* 1000 3600 2))))


(defn create!
  "创建一个用户上下文"
  [user-id]
  (swap! context #(cache/miss % user-id {})))


(defn save-context!
  "保存用户上下文"
  [user-id user-context]
  (swap! context #(cache/miss % user-id user-context)))


(defn get-or-create!
  "获取一个用户的上下文"
  [user-id]
  (let [data (if (cache/has? @context user-id)
               (get @context user-id)
               {})]
    (save-context! user-id data)
    data))


