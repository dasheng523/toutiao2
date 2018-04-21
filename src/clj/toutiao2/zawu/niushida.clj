(ns toutiao2.zawu.niushida
  (:require [toutiao2.driver :as tdriver]
            [etaoin.api :refer :all]
            [etaoin.keys :as ek]
            [clojure.core.async :as async]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [toutiao2.config :refer [isWindows?]]))


(def content-chan (async/chan))

(defn handle-content [content]
  (async/put! content-chan content))

(defn- search-driver []
  (firefox
   {:path-driver (.getPath (io/resource (tdriver/get-firefox-path)))
    :capabilities {}}))

(defn is-has-texts [source ks]
  (if (empty? ks)
    false
    (if (str/includes? source (first ks))
      true
      (recur source (rest ks)))))

(defn souhu-content [driver]
  (get-element-text driver {:css ".article"}))

(defn other-content [driver]
  (get-source driver))

(defn dispatch-content-fn [url]
  (condp #(str/includes? %2 %1) url
    "sohu.com" souhu-content
    other-content))


(defn souhu-search [driver searchkey]
  (go driver (str "http://search.sohu.com/?keyword=" searchkey))
  (loop [i 1]
    (let [detail-node [{:tag :div :class "ArticleFeed"}
                       {:tag :div :index i}
                       {:tag :div :class "ant-card-body"}
                       {:tag :a}]]
      (when (and (exists? driver detail-node)
                 (< i 20))
        (scroll-query driver detail-node)
        (click driver detail-node)
        (wait 1)
        (switch-window driver (last (get-window-handles driver)))
        (wait 5)
        (handle-content (souhu-content driver))
        (close-window driver)
        (switch-window driver (first (get-window-handles driver)))
        (recur (+ i 1))))))


(defn baidu-search [driver searchkey]
  (go driver "https://www.baidu.com")
  (fill driver {:css "input.s_ipt"} searchkey ek/enter))

(defn baidu-current-page [driver index]
  (let [node [{:tag :div :class "result c-container " :index index}
              {:tag :h3}
              {:tag :a}]]
    (when (exists? driver node)
      (scroll-query driver node)
      (click driver node)
      (wait 1)
      (switch-window driver (last (get-window-handles driver)))
      (wait 5)
      (handle-content ((dispatch-content-fn (get-url driver)) driver))
      (close-window driver)
      (switch-window driver (first (get-window-handles driver)))
      (recur driver (+ index 1)))))


#_(def driver (search-driver))

#_(souhu-search driver "纽仕达")
#_(baidu-search driver "奔驰")

#_(baidu-current-page 1)

#_(go driver (str "http://search.sohu.com/?keyword=" "纽仕达"))

#_(for [i (range 20)]
  (let [detail-node {:css (str ".ImageNewsCardContent > a:nth-child(" i ")")}]
    (exists? driver detail-node)))
#_(exists? driver {:css ".ImageNewsCardContent:nth-child(3) > a"})





