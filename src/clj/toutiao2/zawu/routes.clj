(ns toutiao2.zawu.routes
  (:require [toutiao2.layout :as layout]
            [toutiao2.config :refer [env]]
            [compojure.core :as compojure :refer [GET POST]]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :as api]
            [schema.core :as s]
            [toutiao2.zawu.niushida :as niushida]))

(compojure/defroutes zawu-routes
  (GET "/zawu" []
       (layout/render "zawu/index.html")))

(s/defschema app-status
  {:platform String
   :status String})

(s/defschema result-msg
  {:url String
   :badword [String]
   :title String
   :platform s/Keyword})


(api/defapi zawu-service
  {:swagger {:ui "/badword"
             :spec "/badword.json"
             :data {:info {:version "1.0.0"
                           :title "负面信息系统"
                           :description "负面信息系统控制页面"}}}}
  (api/context "/zawu" []
    :tags ["zawu"]

    (api/POST "/login" []
         :return       String
         :query-params []
         :summary      "打开浏览器并登陆"
         (do (niushida/init-app)
             (ok "成功")))
    (api/POST "/start" []
         :return       String
         :query-params []
         :summary      "开始抓取"
         (do (niushida/start-app)
             (ok "正在开始")))
    (api/POST "/stop" []
             :return       String
             :query-params []
             :summary      "终止抓取"
             (do (niushida/stop-app)
                 (ok "正在终止")))
    (api/POST "/status" []
             :return       [app-status]
             :query-params []
             :summary      "任务状态"
             (ok (niushida/app-status)))
    (api/POST "/result-page" []
              :return [result-msg]
              :query-params [page :- Integer, size :- Integer]
              :summary "获取结果页面"
              (ok (niushida/result-page page size)))))
