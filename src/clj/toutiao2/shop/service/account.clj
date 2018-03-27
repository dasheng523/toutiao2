(ns toutiao2.shop.service.account
  (:require [toutiao2.shop.bll.account :as account]
            [toutiao2.shop.logic.caccount :as laccount]
            [toutiao2.shop.common :as common :refer [->success ->fail]]
            [toutiao2.shop.bll.token :as token]
            [toutiao2.shop.bll.trans :refer [tr]]))


(defn auth
  "授权用户"
  [username password token]
  (let [lang (token/get-lang token)]
    (if-let [account (account/auth {:username username :password password})]
      (do (token/set-user-id (laccount/get-account-user-id account) token)
          (->success account))
      (->fail (tr lang :auth-fail)))))


(token/init-token! "111")
(token/get-lang "111")
(auth "dasheng523@163.com" "123456" "111")

