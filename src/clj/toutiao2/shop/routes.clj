(ns toutiao2.shop.routes
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [compojure.api.meta :refer [restructure-param]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth :refer [authenticated?]]
            [compojure.core :as cc]))



#_(:require [toutiao2.layout :as layout]
            [compojure.core :refer [defroutes GET]]
          [ring.util.http-response :as response]
          [clojure.java.io :as io])
#_(defn home-page []
  (layout/render "shop/home.html"))

#_(defroutes shop-routes
  (GET "/shop" []
       (home-page))
  (GET "/shop/test" []
       (response/ok "测试")))


(cc/defroutes shop-routes2
  (GET "/shop/test" []
       (ok "测试")))

(defapi shop-routes
  {:swagger {:ui "/swagger-ui2"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Sample API"
                           :description "Sample Services"}}}}
  (context "/api" []
    :tags ["thingie"]

    (GET "/test" []
         :return       String
         :query-params [x :- Long, {y :- Long 1}]
         :summary      "x+y with query-parameters. y defaults to 1."
         (ok "你好继续"))

    (GET "/power" []
      :return      Long
      :header-params [x :- Long, y :- Long]
      :summary     "x^y with header-parameters"
      (ok (long (Math/pow x y))))))
