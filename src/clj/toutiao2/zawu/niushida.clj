(ns toutiao2.zawu.niushida
  (:require [toutiao2.driver :as tdriver]
            [etaoin.api :refer :all]
            [etaoin.keys :as ek]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [toutiao2.config :refer [isWindows?]]))

(defn- search-driver []
  (chrome
   {:path-driver (.getPath (io/resource (tdriver/get-chromedriver-path)))
    :capabilities {}}))

(defn is-has-texts [source ks]
  (if (empty? ks)
    false
    (if (str/includes? source (first ks))
      true
      (recur source (rest ks)))))

(defn souhu-search [driver searchkey]
  (go driver (str "http://search.sohu.com/?keyword=" searchkey))
  (for [i (range 20)]
    (let [detail-node {:css (str ".ImageNewsCardContent:nth-child(" i ") > a")}]
      (when (exists? driver detail-node)
        (scroll-query driver detail-node)
        (click driver detail-node)
        (switch-window driver (last (get-window-handles driver)))
        (let [content (get-element-text driver {:css ".article"})]
          (switch-window driver (first (get-window-handles driver)))
          content)))))



#_(def driver (search-driver))
#_(souhu-search driver "纽仕达")

#_(go driver (str "http://search.sohu.com/?keyword=" "纽仕达"))

#_(for [i (range 20)]
  (let [detail-node {:css (str ".ImageNewsCardContent > a:nth-child(" i ")")}]
    (exists? driver detail-node)))
#_(exists? driver {:css ".ImageNewsCardContent:nth-child(3) > a"})
#_(switch-window driver (last (get-window-handles driver)))




