(ns toutiao2.shop.bll.account
  (:require
   [toutiao2.shop.logic.caccount :as calogic]
   [toutiao2.shop.logic.user :as userlogic]
   [toutiao2.shop.cache :as cache]))

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

(defn login
  "登陆"
  [{:keys [email password session-id]}]
  (when-let [account (calogic/authenticate-account email password)]
    (cache/set-cache! (str "login-user:" session-id)
                      (calogic/get-account-user-id account))
    true))


(defn is-login?
  "判断是否已登陆"
  [session-id]
  (cache/get-cache (str "login-user:" session-id)))

(register {:email "dasheng5231@163.com" :password "a5235013"})
(login {:email "dasheng5231@163.com"
        :password "a5235013"
        :session-id "11223344"})
(is-login? "11223344")

