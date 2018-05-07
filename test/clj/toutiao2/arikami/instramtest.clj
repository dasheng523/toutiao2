(ns toutiao2.arikami.instramtest
  (:require  [clojure.test :as t]
             [toutiao2.arikami.instagram :refer :all]
             [etaoin.api :refer :all]))


(def driver (instagram-driver))
(quit driver)

(recovery-login driver)
(do-follow-user driver)
#_(comment-user driver)


(defn get-image-dime [file]
  (imgresize/dimensions (ImageIO/read file)))


#_(do-follow-user driver)
#_(comment-index driver 20)


