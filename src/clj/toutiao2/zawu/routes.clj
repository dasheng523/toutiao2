(ns toutiao2.zawu.routes
  (:require [toutiao2.layout :as layout]
            [toutiao2.config :refer [env]]
            [compojure.core :as compojure :refer [GET POST]]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :as api]
            [compojure.api.meta :refer [restructure-param]]))

(compojure/defroutes zawu-routes
  (GET "/zawu" []
       (layout/render "zawu/index.html")))

(api/defapi zawu-service
  {:swagger {:ui "/badword-manger"
             :spec "/badword.json"
             :data {:info {:version "1.0.0"
                           :title "Sample API"
                           :description "Sample Services"}}}}

  (api/context "/api" []
    :tags ["shop"]

    (api/GET "/test" []
         :return       String
         :query-params [x :- Long, {y :- Long 1}]
         :summary      "测试"
         (ok "你好"))
    (api/GET "/auth" []
         :return       String
         :query-params [username :- String, password :- String]
         :summary      "用户授权"
         (ok {:code 200 :message "success" :data {:uname "5566"}}))))
