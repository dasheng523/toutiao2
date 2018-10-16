(ns toutiao2.logic.jianshu
  (:require [toutiao2.db.core :as db]
            [toutiao2.http :as http]
            [toutiao2.html-parser :as html]
            [toutiao2.config :as config :refer [env]]))

(def cookies (-> env :jianshu-cookies))

(def do-get (fn [url]
              ((http/compose-get
                cookies
                #()
                #()))))

;; 获取列表页
