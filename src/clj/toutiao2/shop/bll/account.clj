(ns toutiao2.shop.bll.account
  (:require
   [toutiao2.shop.logic.common-account :as calogic]
   [toutiao2.shop.logic.user :as userlogic]))

(defn register
  "注册用户"
  [{:keys [email password]}]
  (assert (empty? (userlogic/get-user-by-email email)) "该用户已存在")
  (let [user (userlogic/create-user {:email email})
        account (calogic/create-account {:user_id (userlogic/get-user-id user)
                                         :username email
                                         :password password})]
    (calogic/save-account! account)
    (userlogic/save-user! user)))

#_(register {:email "dasheng5231@163.com" :password "a5235013"})



