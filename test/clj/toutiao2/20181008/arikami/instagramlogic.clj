(ns toutiao2.arikami.instagramlogic
  (:require [toutiao2.driver :as tdriver]
            [clj-webdriver.taxi :refer :all]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clj-webdriver.driver :refer [init-driver]])
  (:import (org.openqa.selenium.chrome ChromeDriver ChromeOptions)
           (org.openqa.selenium By)
           (org.openqa.selenium.remote LocalFileDetector RemoteWebElement)))


(defn create-chrome-proxy-driver []
  (System/setProperty "webdriver.chrome.driver"
                      (.getPath (io/resource (tdriver/get-chromedriver-path))))
  (init-driver {:webdriver
                (ChromeDriver.
                 (doto (ChromeOptions.)
                   (.addArguments ["--user-agent=Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Mobile Safari/537.36"
                                   "--proxy-server=socks5://127.0.0.1:55555"
                                   "--window-size=375,667"])))}))


(defn instagram-home [driver]
  (to driver "https://www.instagram.com/"))



#_(def driver (create-chrome-proxy-driver))
#_(instagram-home driver)

#_(click driver "div.coreSpriteFeedCreation")

#_(let [rawdriver (:webdriver driver)
      path "f://20180414122806.png"
      detector (LocalFileDetector.)
      node (.findElement rawdriver (By/className "_l8al6"))
      ff (.getLocalFile detector (into-array [path]))]
  (.setFileDetector (case RemoteWebElement node) detector)
  (send-keys driver "form._7xah4 input._l8al6" path)
  #_(.sendKeys node (.getAbsolutePath ff)))

#_(execute-script driver "HTMLInputElement.prototype.click = function() {if(this.type !== 'file') HTMLElement.prototype.click.call(this);}")
