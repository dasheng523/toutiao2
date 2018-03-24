(ns toutiao2.shop.bll.token
  (:require [toutiao2.shop.cache :as cache]))

(defn get-user-id [token]
  (cache/get-cache-live! cache/token-cache (str "user-id" token)))

(defn get-lang [token])
