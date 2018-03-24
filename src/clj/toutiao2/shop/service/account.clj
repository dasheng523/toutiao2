(ns toutiao2.shop.service.account
  (:require [toutiao2.shop.bll.account :as account]
            [toutiao2.shop.logic.user :as luser]
            [toutiao2.shop.common :as common]
            [toutiao2.shop.bll.token :as btoken]
            [toutiao2.shop.cache :as cache]
            [toutiao2.shop.bll.trans :reffer [__]]))

(defn auth
  "授权用户"
  [username password token]
  (let [lang (cache/get-token-data token :user-id)]
    (if (account/auth {:username username :password password})

      (common/->fail (__ lang :fail))))

  
  (let [getinfo #(some-> %
                         (account/token->user-id)
                         (luser/get-by-id)
                         (luser/get-user-data [:first-name :last-name]))]
    ))

