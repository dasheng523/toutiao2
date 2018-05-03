(ns toutiao2.arikami.instagram
  (:require [toutiao2.driver :as tdriver]
            [etaoin.api :refer :all]
            [etaoin.keys :as ek]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [toutiao2.config :refer [isWindows?]]
            [clojure.tools.logging :as log]
            [image-resizer.core :as imgresize]
            [image-resizer.format :as format])
  (:import [javax.imageio ImageIO]))

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
    "e:/coo/"
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

(defn follow-user-index
  ([driver index]
   (when (and (exists? driver {:css (str "._ezgzd:nth-child(" index ")")})
              (< index 15))
     (if (exists? driver {:css (str "._ezgzd:nth-child(" (- index 2) ")")})
       (scroll-query driver {:css (str "._ezgzd:nth-child(" (- index 2) ")")}))
     (click driver [{:css (str "._ezgzd:nth-child(" index ") a")}])
     (wait-exists driver {:css "span._ov9ai"})
     (wait 3)
     (when (exists? driver {:css "span._ov9ai button._gexxb"})
       (click driver {:css "span._ov9ai button._gexxb"})
       (wait 2))
     (back driver)
     (wait 2)
     (recur driver (+ index 1))))
  ([driver]
   (follow-user-index driver 3)))



(defn do-follow-user [driver]
  (click driver {:css "._k0d2z:nth-child(2) a"})
  (wait-exists driver {:css "._6d3hm"})
  (doseq [x (range 1 4)]
    (click driver
           {:css
            (str "._6d3hm:nth-child(1) ._mck9w:nth-child(1) a")})
    (wait 2)
    (follow-user-index driver)
    (back driver)
    (refresh driver)
    (wait-enabled driver {:css
                          (str "._6d3hm:nth-child(1) ._mck9w:nth-child(1) a")})))

(defn random-greet []
  (rand-nth ["hello, it is good!"
             "Awwwwwww"
             "Kindly follow back"
             "I love your pics"
             "Beauty"
             "This is what I want to be seeing from now sweetheart."
             "looks good"
             "yes cee c do u really love"
             "Oh Nice"
             "Wow, is this an exercise or something?"
             "Amazing look"]))


(defn comment-index
  ([driver index]
   (when (and (exists? driver {:css (str "._6e4x5:nth-child(" index ")" " a._2g7d5")})
              (< index 15))
     (scroll-query driver {:css (str "._6e4x5:nth-child(" index ")" " a._2g7d5")})
     (scroll-by driver 0 -40)
     (click driver {:css (str "._6e4x5:nth-child(" index ")" " a._2g7d5")})
     (wait (+ (rand-int 10) 1))
     (try
       (wait-exists driver {:css "._mck9w"} {:timeout 10})
       (catch Exception e
         (log/error e)))
     (when (and (exists? driver {:css "._mck9w"})
                (not (exists? driver {:css "._7r25s"})))
       (click driver {:css "._mck9w"})
       (wait (+ (rand-int 10) 1))
       (try
         (wait-enabled driver {:css ".coreSpriteComment"})
         (catch Exception e
           (log/error e)))
       (when (exists? driver {:css ".coreSpriteComment"})
         (scroll-query driver {:css ".coreSpriteComment"})
         (scroll-by driver 0 -40)
         (when-not (exists? driver {:css "._reoub"})
           (click driver {:css ".coreSpriteComment"})
           (fill-human driver {:css "textarea._bilrf"} (random-greet))
           (click driver {:css "._55p7a"})
           (wait-exists driver {:css "._reoub"})))
       (back driver))
     (back driver)
     (wait 2)
     (recur driver (+ 1 index))))
  ([driver]
   (comment-index driver 1)))

(defn comment-user [driver]
  (click driver {:css "._k0d2z:nth-child(5) a"})
  (wait-exists driver {:css "._573jb:nth-child(3)"})
  (click driver {:css "._573jb:nth-child(3)"})
  (wait 3)
  (comment-index driver))

(defn auto-do [driver]
  (instagram-home driver)
  (recovery-cookies driver "dasheng523@163.com")
  (instagram-home driver))

(def driver (instagram-driver))
#_(save-cookies driver "dasheng523@163.com")


(auto-do driver)
(do-follow-user driver)
#_(comment-user driver)


(defn get-image-dime [file]
  (imgresize/dimensions (ImageIO/read file)))


#_(do-follow-user driver)
#_(comment-index driver 20)




#_(js-execute driver "document.querySelector('form._7xah4 input._l8al6').disabled=true;")
#_(click driver {:tag :div :class "_crp8c coreSpriteFeedCreation"})
#_(upload-file driver {:css "form._7xah4 input._l8al6"} "f:/20180414122806.png")

#_(instagram-home driver)
#_(save-cookies driver "dasheng523@163.com")
#_(recovery-cookies driver "dasheng523@163.com")
#_(instagram-home driver)
#_(post-image driver "/Users/huangyesheng/Desktop/222.jpg" "#skirt")
#_(follow-user-index 3)




