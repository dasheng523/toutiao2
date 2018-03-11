(ns toutiao2.shop.account
  (:require
   [toutiao2.shop.db :refer :all]
   [toutiao2.utils :as utils]
   [honeysql.helpers :refer :all :as helpers]
   [honeysql.core :as sql]
   [clojure.java.jdbc :as jdbc]
   [clojure.tools.logging :as log]))

(defprotocol Account
  (save [account])
  (reset-password [accountt password]))

(defprotocol AuthRequst
  (authenticate [req]))

(defn- encrypt
  "加密"
  [s seed]
  (utils/md5 (str s seed)))

(defn- get-common-account-by-name
  "通过用户名获取普通账号"
  [username]
  (-> (select :*)
      (from :common_account)
      (where [:= :username username])
      (sql-query)
      (first)))

(defn- authenticate-common-account
  "普通的用户名密码类型的验证"
  [username password]
  (if-let [account (get-common-account-by-name username)]
    (-> (select :id)
        (from :common_account)
        (where [:= :username username]
               [:= :password (encrypt password (:seed account))]
               [:= :is_active 1])
        (sql-query)
        (first))))


(defn- save-common-account!
  "保存授权账号"
  [account]
  (save-simple-data! :common_account account :username))


(defn create-common-account
  "创建普通的授权账号"
  [{:keys [user_id username password is_active]
    :or {is_active true}}]
  (let [seed (utils/rand-string)
        account {:username username
                 :password (encrypt password seed)
                 :user_id user_id
                 :seed seed
                 :is_active is_active
                 :id (utils/rand-idstr)}]
    ^{:type :common} account))


(defn- change-password-common
  "更改密码"
  [account password]
  (assoc account
         :password
         (encrypt password (:seed account))))


(defmacro defabc [name ])

(defabc abc
  ([a b] (+ a b))
  ([b d] (- b d)))

(abc {:a 1 :b 2})  ;; = 3
(abc {:b 2 :d 2})  ;; = 0

(defmulti authenticate
  (fn [auth-request] (vec (keys auth-request))))
(defmethod authenticate [:username :password]
  [{:keys [username password]}]
  (authenticate-common-account username password))

#_(authenticate {:username "test" :password "7788"})


#_(->
 (create-common-account {:username "test" :password "test55663" :user_id "1"})
 (change-password-common "7788")
 (save-common-account!))

#_(authenticate-common-account "test" "7788")



