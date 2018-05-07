(ns toutiao2.zawu.routes
  (:require [toutiao2.layout :as layout]
            [toutiao2.config :refer [env isWindows?]]
            [compojure.core :as compojure :refer [GET POST]]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :as api]
            [schema.core :as s]
            [toutiao2.zawu.niushida :as niushida]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [muuntaja.core :as muuntaja])
  (:import (java.io File)))

(def mycharset (if (isWindows?) "gbk" "utf-8"))

(compojure/defroutes zawu-routes
  (GET "/zawu" []
       (-> (layout/render "zawu/index.html")
           (charset mycharset)))
  (POST "/zawu/download" []
       (-> (niushida/create-markbad-file)
           (file-response)
           (header "Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))))

(s/defschema app-status
  {:platform String
   :status String})

(s/defschema result-msg
  {:url String
   :id String
   :badword [String]
   :ctime s/Num
   :pagetime String
   :isbad Boolean
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
             (ok "ok")))
    (api/POST "/start" []
         :return       String
         :form-params [kwords :- String]
         :summary      "开始抓取"
         (do (niushida/start-app (json/parse-string kwords true))
             (ok "正在开始")))
    (api/POST "/stop" []
             :return       String
             :query-params []
             :summary      "终止抓取"
             (do (niushida/stop-app)
                 (ok "正在终止")))
    (api/POST "/markbad" []
              :return       String
              :form-params [id :- String, del :- Boolean]
              :summary      "标记负面信息"
              (do (niushida/mark-bad id del)
                  (ok "ok")))
    (api/POST "/status" []
             :return       [app-status]
             :query-params []
             :summary      "任务状态"
             (ok (niushida/app-status)))
    #_(api/POST "/download" []
              :return File
              :produces ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"]
              (-> (niushida/create-markbad-file)
                  #_(io/resource "docs/docs.md")
                  (file-response)
                  #_(io/input-stream)
                  #_(ok)
                  #_(header "Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                  (muuntaja/disable-response-encoding)))
    (api/POST "/result-page" []
              :return [result-msg]
              :form-params [page :- s/Num]
              :summary "获取结果页面"
              (ok (niushida/result-page page 10)))))

