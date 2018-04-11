(ns toutiao2.shop.cache
  (:require [toutiao2.cache :refer :all]))

; 一些正在用的缓存
(def user-token-cache (create-cache))
(def token-cache (create-cache))

