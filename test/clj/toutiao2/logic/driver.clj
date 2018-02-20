(ns toutiao.logic.driver
  (:require [etaoin.api :refer :all]
            [clojure.java.io :as io]
            [toutiao.config :refer [env]]))

;; 谷歌浏览器配置
(def default-config
  {:path-driver (.getPath (io/resource (env :chrome-driver)))})

(def proxy-config
  {:path-driver (.getPath (io/resource (get-driver-path)))
   :capabilities {:chromeOptions {:args ["--proxy-server=localhost:12345"]}}})

(defn create-default-browser
  "创建一个谷歌浏览器实例"
  []
  (chrome default-config))


;; 火狐浏览器配置
(def default-firefox-config
  {:path-driver (.getPath (io/resource (env :firefox-driver)))})

(defn create-default-firfox-config
  "创建火狐浏览器"
  []
  (firefox default-firfox-config))

