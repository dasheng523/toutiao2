(ns toutiao2.arikami.lofter
  (:require [toutiao2.driver :as tdriver]
            [etaoin.api :refer :all]
            [etaoin.keys :as ek]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [toutiao2.config :refer [isWindows?]]))

(defn- lofter-driver []
  (chrome
   {:path-driver (.getPath (io/resource (tdriver/get-chromedriver-path)))
    :capabilities {:chromeOptions {:args ["--user-agent=Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Mobile Safari/537.36"]}}}))
