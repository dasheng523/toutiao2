(ns toutiao2.shop.logic.user
  (:require
   [toutiao2.shop.logic.db :refer :all]
   [toutiao2.utils :as utils]
   [clj-time.local :as ltime]
   [clj-time.core :as t]
   [honeysql.core :as sql]
   [clojure.string :as str]))


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

(defget user [:email :id :name])


