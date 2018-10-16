(ns toutiao2.shop.logic.facebookaccount
  (:require [toutiao2.utils :as utils]
            [toutiao2.shop.logic.db :refer :all]))

(defn create-account
  [{:keys [user-id facebook-user-id]}]
  (let [id (utils/rand-idstr)]
    {:id id
     :user-id user-id
     :facebook-user-id facebook-user-id
     :is_active true}))

(defn save-account!
  [account]
  (save-simple-data! :facebook_account
                     (->dbmap account)
                     :user_id))

(defn get-by-user-id
  [user-id]
  (get-first :facebook_account :user_id user-id))

(defn get-by-facebook-id
  [facebook-id]
  (get-first :facebook_account :facebook_user_id facebook-id))

(defget account [:facebook-user-id :user-id])
