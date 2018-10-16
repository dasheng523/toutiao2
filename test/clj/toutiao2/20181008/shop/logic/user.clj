(ns toutiao2.shop.logic.user
  (:require
   [toutiao2.shop.logic.db :refer :all]
   [toutiao2.utils :as utils]
   [clj-time.local :as ltime]
   [clj-time.core :as t]
   [honeysql.core :as sql]
   [clojure.string :as str]))

(defget user [:email :id :first-name :last-name])

(defn create-user
  "创建用户"
  [{:keys [email first-name last-name address sex phone is-active create-time]
    :or {is-active true
         create-time (ltime/local-now)}
    :as input-data}]
  (assoc (-> input-data ->dbmap)
         :id (utils/rand-idstr)))

(defn save-user!
  "保存用户"
  [user]
  (save-simple-data! :users user :id))

(defn get-user-by-email [email]
  (get-first :users :email email))

(defn get-by-id [user-id]
  (get-first :users :id user-id))


(defn get-user-field
  "获取用户某个字段"
  [user field]
  (get user field))


(defn get-user-data
  "通过user-id和字段获取user的数据"
  [user-info fields]
  (reduce #(assoc %1 %2 (get-user-field user-info %2)) {} fields))
