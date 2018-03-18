(ns toutiao2.shop.routes
  (:require [toutiao2.layout :as layout]
            [toutiao2.shop.logic.caccount :as ca]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))


(defn home-page []
  (layout/render "shop/index.html"))

(defroutes shop-routes
  (GET "/shop" []
       (home-page))
  (POST "/shop/login-post" [username password]
        (ca/authenticate-account username password)
        (response/ok "111"))
  (GET "/shop/test" []
       (response/ok "测试")))

