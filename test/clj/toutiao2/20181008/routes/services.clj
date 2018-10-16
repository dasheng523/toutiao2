(ns toutiao2.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [compojure.api.meta :refer [restructure-param]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth :refer [authenticated?]]
            [toutiao2.shop.schema :as ss]
            [toutiao2.zimeiti.toutiao :as toutiao]))

(defn access-error [_ _]
  (unauthorized {:error "unauthorized"}))

(defn wrap-restricted [handler rule]
  (restrict handler {:handler  rule
                     :on-error access-error}))

(defmethod restructure-param :auth-rules
  [_ rule acc]
  (update-in acc [:middleware] conj [wrap-restricted rule]))

(defmethod restructure-param :current-user
  [_ binding acc]
  (update-in acc [:letks] into [binding `(:identity ~'+compojure-api-request+)]))


(defapi service-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Sample API"
                           :description "Sample Services"}}}}

  (GET "/authenticated" []
       :auth-rules authenticated?
       :current-user user
       (ok {:user user}))

  (context "/api" []
    :tags ["shop"]

    (GET "/test" []
         :return       String
         :query-params [x :- Long, {y :- Long 1}]
         :summary      "测试"
         (ok "你好"))
    (GET "/auth" []
         :return       ss/RespAuth
         :query-params [username :- String, password :- String]
         :summary      "用户授权"
         (ok {:code 200 :message "success" :data {:uname "5566"}})))

  (context "/toutiao" []
           :tags ["toutiao"]

           (POST "/open" []
                :return       String
                :query-params []
                :summary      "打开头条登陆页"
                (do (toutiao/open-chrome)
                    (ok "ok")))
           (POST "/save-login" []
                :return       String
                :query-params [username :- String]
                :summary      "保存登陆状态"
                (do (toutiao/do-save-cookies username)
                    (ok "ok")))
           (POST "/recover-login" []
                 :return       String
                 :query-params [username :- String]
                 :summary      "恢复登陆状态"
                 (do (toutiao/do-recover-cookies username)
                     (ok "ok")))
           (POST "/autorun" []
                :return       String
                :query-params [urls :- [String]]
                :summary      "执行自动任务"
                (do (toutiao/doautorun urls)
                    (ok "ok"))))

  (context "/test" []
    :tags ["thingie"]

    (GET "/test" []
         :return       String
         :query-params [x :- Long, {y :- Long 1}]
         :summary      "x+y with query-parameters. y defaults to 1."
         (ok "你好"))

    (GET "/plus" []
      :return       Long
      :query-params [x :- Long, {y :- Long 1}]
      :summary      "x+y with query-parameters. y defaults to 1."
      (ok (+ x y)))

    (POST "/minus" []
      :return      Long
      :body-params [x :- Long, y :- Long]
      :summary     "x-y with body-parameters."
      (ok (- x y)))

    (GET "/times/:x/:y" []
      :return      Long
      :path-params [x :- Long, y :- Long]
      :summary     "x*y with path-parameters"
      (ok (* x y)))

    (POST "/divide" []
      :return      Double
      :form-params [x :- Long, y :- Long]
      :summary     "x/y with form-parameters"
      (ok (/ x y)))

    (GET "/power" []
      :return      Long
      :header-params [x :- Long, y :- Long]
      :summary     "x^y with header-parameters"
      (ok (long (Math/pow x y))))))
