(ns toutiao2.arikami.instagram
  (:require [toutiao2.driver :as tdriver]
            [etaoin.api :refer :all]
            [etaoin.keys :as ek]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [toutiao2.config :refer [isWindows?]]))


"--proxy-server=socks5://127.0.0.1:55555"
(defn- instagram-driver []
  (chrome
   {:path-driver (.getPath (io/resource (tdriver/get-chromedriver-path)))
    ;:size [375 667]
    :capabilities {:chromeOptions {:args ["--user-agent=Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Mobile Safari/537.36"
                                          "--window-size=375,667"]}}}))


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
    "/Users/huangyesheng/Documents/cookies"))

(defn save-cookies [driver user]
  (let [coo (get-cookies driver)]
    (spit (str (get-cookies-path) user ".cookies") (json/generate-string coo))))

(defn recovery-cookies [driver user]
  (let [coos (-> (str (get-cookies-path) user ".cookies")
                (slurp)
                (json/parse-string true))]
    (doseq [coo coos]
      (set-cookie driver coo))))


(defn post-image [driver image-path desc]
  (js-execute driver "document.querySelector('form._7xah4 input._l8al6').disabled=true;")
  (click driver {:tag :div :class "_crp8c coreSpriteFeedCreation"})
  (upload-file driver {:css "form._7xah4 input._l8al6"} image-path)
  (wait-exists driver {:tag :button :class "_9glb8"})
  (with-wait 2
    (if (exists? driver {:css "button._j7nl9"})
      (click driver {:css "button._j7nl9"}))
    (click driver {:tag :button :class "_9glb8"})
    (wait-exists driver {:css "textarea._qlp0q"})
    (fill-human driver {:css "textarea._qlp0q"} desc)
    (click driver {:css "button._9glb8"})))

(defn follow-user-index [driver index]
  (when (exists? driver {:css (str "li._ezgzd:nth-child(" index ")")})
    (scroll-query driver {:css (str "li._ezgzd:nth-child(" index ")")})
    (click driver [{:css (str "li._ezgzd:nth-child(" index ") a")}])
    (wait-exists driver {:css "span._ov9ai"})
    (wait 3)
    (when (exists? driver {:css "span._ov9ai button._gexxb"})
      (click driver {:css "span._ov9ai button._gexxb"})
      (wait 2))
    (back driver)
    (wait 2)
    (recur driver (+ index 1))))

#_(js-execute driver "document.querySelector('form._7xah4 input._l8al6').disabled=true;")
#_(click driver {:tag :div :class "_crp8c coreSpriteFeedCreation"})
#_(upload-file driver {:css "form._7xah4 input._l8al6"} "f:/20180414122806.png")

#_(def driver (instagram-driver))
#_(instagram-home driver)
#_(save-cookies driver "dasheng523@163.com")
#_(recovery-cookies driver "dasheng523@163.com")
#_(instagram-home driver)
#_(post-image driver "/Users/huangyesheng/Desktop/222.jpg" "#skirt")
#_(follow-user-index 3)




