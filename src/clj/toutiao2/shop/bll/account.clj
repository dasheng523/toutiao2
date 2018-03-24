(ns toutiao2.shop.bll.account
  (:require
   [toutiao2.shop.logic.caccount :as calogic]
   [toutiao2.shop.logic.facebookaccount :as fklogic]
   [toutiao2.shop.logic.user :as userlogic]
   [toutiao2.shop.cache :as cache]
   [toutiao2.utils :as utils]))

(defn register
  "注册用户"
  [{:keys [email password]}]
  (assert (empty? (userlogic/get-user-by-email email)) "该用户已存在")
  (let [user (userlogic/create-user {:email email})
        account (calogic/create-account {:user-id (userlogic/get-user-id user)
                                         :username email
                                         :password password})]
    (calogic/save-account! account)
    (userlogic/save-user! user)))

(defn auth
  "登陆"
  [{:keys [email password]}]
  (when-let [account (calogic/authenticate-account email password)]
    (let [token (utils/rand-idstr)]
      (cache/set-cache! cache/user-token-cache token (calogic/get-account-user-id account))
      token)))

(defn token->user-id
  "通过token转换userid"
  [token]
  (cache/get-or-create-live! cache/token-cache token))


(defn register-facebook
  "注册facebook用户"
  [{:keys [facebook-user-id first-name last-name]}]
  (assert (empty? (fklogic/get-by-facebook-id facebook-user-id)) "该用户已存在")
  (let [user (userlogic/create-user {:first-name first-name :last-name last-name})
        account (fklogic/create-account {:user-id (userlogic/get-user-id user)
                                         :facebook-user-id facebook-user-id})]
    (fklogic/save-account! account)
    (userlogic/save-user! user)))

