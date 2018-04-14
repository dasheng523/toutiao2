(ns toutiao2.driver
  (:require [etaoin.api :refer :all]
            [clojure.java.io :as io]
            [toutiao2.config :refer [env isWindows?]]))

(defn get-chromedriver-path []
  (if (isWindows?)
    "driver/chromedriver.exe"
    "driver/chromedriver"))

(defn get-firefox-path []
  (if (isWindows?)
    "driver/geckodriver.exe"
    "driver/geckodriver"))


;; 谷歌标准配置
(def default-chrome-config
  {:path-driver (.getPath (io/resource (get-chromedriver-path)))})

;; 谷歌代理配置
(def proxy-chrome-config
  {:path-driver (.getPath (io/resource (get-chromedriver-path)))
   :capabilities {:chromeOptions {:args ["--proxy-server=socks5://127.0.0.1:55555"]}}})

#_(def proxy-chrome-config
  {:path-driver (.getPath (io/resource (get-chromedriver-path)))
   :capabilities {:chromeOptions {:args ["--proxy-server=socks5://user:pass@127.0.0.1:55555"]}}})

(defn create-default-browser
  "创建一个谷歌浏览器实例"
  []
  (chrome default-chrome-config))

(defn create-proxy-browser
  "创建一个代理浏览器"
  []
  (chrome proxy-chrome-config))


;; 火狐浏览器配置
(def default-firefox-config
  {:path-driver (.getPath (io/resource (get-firefox-path)))})

(defn create-default-firfox
  "创建火狐浏览器"
  []
  (firefox default-firefox-config))

#_(def dd (create-proxy-browser))
#_(go dd "http://www.ip138.com/")
#_(quit dd)
