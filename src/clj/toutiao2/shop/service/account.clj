(ns toutiao2.shop.service.account
  (:require [toutiao2.shop.bll.account :as account]
            [toutiao2.shop.logic.user :as luser]))

(defn auth
  "授权用户"
  [username password]
  (if-let [token (account/auth {:username username :password password})]
    (if-let [user-info (some-> token
                        (account/token->user-id)
                        (luser/get-by-id)
                        (luser/get-user-data [:first-name :last-name]))])))


