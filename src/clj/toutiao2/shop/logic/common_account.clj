(ns toutiao2.shop.logic.common-account
  (:require
   [toutiao2.shop.logic.db :refer :all]
   [toutiao2.utils :as utils]
   [honeysql.helpers :refer :all :as helpers]
   [honeysql.core :as sql]
   [clojure.java.jdbc :as jdbc]
   [clojure.tools.logging :as log]))

(defn- encrypt
  "加密"
  [s seed]
  (utils/md5 (str s seed)))

(defn get-account-by-name
  "通过用户名获取普通账号"
  [username]
  (-> (select :*)
      (from :common_account)
      (where [:= :username username])
      (sql-query)
      (first)))

(defn authenticate-account
  "普通的用户名密码类型的验证"
  [username password]
  (if-let [account (get-account-by-name username)]
    (-> (select :id)
        (from :common_account)
        (where [:= :username username]
               [:= :password (encrypt password (:seed account))]
               [:= :is_active 1])
        (sql-query)
        (first))))


(defn save-account!
  "保存授权账号"
  [account]
  (save-simple-data! :common_account account :username))


(defn create-account
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


(defn change-password
  "更改密码"
  [account password]
  (assoc account
         :password
         (encrypt password (:seed account))))
