(ns toutiao2.cgkc.grap
  (:require #_[clj-webdriver.taxi :refer :all]
            #_[clj-webdriver.driver :refer [init-driver]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [slingshot.slingshot :refer [throw+ try+]]
            [clj-time.local :as l-t]
            [net.cgrand.enlive-html :as enlive]
            [clojure.tools.logging :as log]))

