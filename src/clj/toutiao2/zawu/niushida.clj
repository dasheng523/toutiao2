(ns toutiao2.zawu.niushida
  (:require [toutiao2.driver :as tdriver]
            [etaoin.api :refer :all]
            [etaoin.keys :as ek]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [toutiao2.config :refer [isWindows?]]))

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
  (for [i (range 20)]
    (let [detail-node {:css (str ".ImageNewsCardContent:nth-child(" i ") > a")}]
      (when (exists? driver detail-node)
        (scroll-query driver detail-node)
        (click driver detail-node)
        (switch-window driver (last (get-window-handles driver)))
        (let [content (souhu-content driver)]
          (switch-window driver (first (get-window-handles driver)))
          content)))))

(defn baidu-search [driver searchkey]
  (go driver "https://www.baidu.com")
  (fill driver {:css "input.s_ipt"} searchkey ek/enter))

(def driver (search-driver))
(baidu-search driver "奔驰")

(defn baidu-current-page [index]
  (let [node [{:tag :div :class "result c-container " :index index} {:tag :h3} {:tag :a}]]
    (when (exists? driver node)
      (click driver node)
      (switch-window driver (last (get-window-handles driver)))
      (let [content (get-element-text driver {:css ".article"})]
        (switch-window driver (first (get-window-handles driver)))
        content))))




(click driver [{:tag :div :class "result c-container " :index 4} {:tag :h3} {:tag :a}])

(switch-window driver (last (get-window-handles driver)))

(fill-human driver {:css ".s_ipt"} "qq")


#_(souhu-search driver "纽仕达")
#_(go driver (str "http://search.sohu.com/?keyword=" "纽仕达"))

#_(for [i (range 20)]
  (let [detail-node {:css (str ".ImageNewsCardContent > a:nth-child(" i ")")}]
    (exists? driver detail-node)))
#_(exists? driver {:css ".ImageNewsCardContent:nth-child(3) > a"})





