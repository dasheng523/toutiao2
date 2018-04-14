(ns toutiao2.arikami.instagram
  (:require [toutiao2.driver :as tdriver]
            [etaoin.api :refer :all]
            [etaoin.keys :as ek]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clj-webdriver.taxi :as d]
            [toutiao2.config :refer [isWindows?]]))


(defn- instagram-driver []
  (chrome
   {:path-driver (.getPath (io/resource (tdriver/get-chromedriver-path)))
    :size [375 667]
    :capabilities {:chromeOptions {:args ["--proxy-server=socks5://127.0.0.1:55555"
                                          "--user-agent=Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Mobile Safari/537.36"]}}}))


(defn instagram-home [driver]
  (go driver "https://www.instagram.com/"))

(defn login [driver user pass]
  (if (exists? driver {:tag :a :href "/accounts/login/"})
    (click driver {:tag :a :href "/accounts/login/"})
    (click driver {:tag :button :fn/text "Log In"}))
  (fill-human driver {:tag :input :name "username"} user)
  (fill-human driver {:tag :input :name "password"} pass)
  (fill driver {:tag :input :name "password"} ek/enter))


(defn- get-cookies-path []
  (if (isWindows?)
    "g:/coo/"
    "/Users/"))

(defn save-cookies [driver user]
  (let [coo (get-cookies driver)]
    (spit (str (get-cookies-path) user ".cookies") (json/generate-string coo))))

(defn recovery-cookies [driver user]
  (let [coos (-> (str (get-cookies-path) user ".cookies")
                (slurp)
                (json/parse-string true))]
    (doseq [coo coos]
      (set-cookie driver coo))))


(defn post-image [driver image-path]
  (js-execute driver "HTMLInputElement.prototype.click = function() {if(this.type !== 'file') HTMLElement.prototype.click.call(this);}")
  (click driver {:tag :div :class "_crp8c coreSpriteFeedCreation"})
  (upload-file driver {:css "form._7xah4 input._l8al6"} "f:/20180414122806.png")
  (click driver {:tag :button :class "_9glb8"})
  (fill-human driver {:css "textarea._qlp0q"} "hello world")
  (click driver {:css "button._9glb8"}))

(def driver (instagram-driver))
(instagram-home driver)
(save-cookies driver "dasheng523@163.com")
(recovery-cookies driver "dasheng523@163.com")
(instagram-home driver)





