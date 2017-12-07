(ns toutiao2.api.qier-api
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [toutiao2.utils :as utils]
            [clojure.spec.test.alpha :as stest]))

(s/check-asserts true)

(def client-id "524ed579c87ddb2dcd0bfd197d321547")
(def client-secret "515def9db75e46f9fe9ee2c815d93b46ca1919fb")
(def access-token-url (str "https://auth.om.qq.com/omoauth2/accesstoken?grant_type=clientcredentials&client_id=" client-id "&client_secret=" client-secret))
(def post-article-url "https://api.om.qq.com/article/clientpubpic")
(def token-info (atom []))

(defn reset-token! [token-data]
  (reset! token-info (assoc token-data :ctime (System/currentTimeMillis))))

(defn current-token []
  (let [now (System/currentTimeMillis)
        ctime (:ctime @token-info)
        expire (:expires_in @token-info)]
    (if (> (- now ctime) expire)
      (:access_token @token-info)
      [reset-token! []])))


(defn exec [])

(defn queue [])

(defn insure-token []
  (:token queue))


(defn parse-resp
  [resp]
  (-> resp
      (json/parse-string true)
      (->> (s/assert :toutiao2.api.qier-spec/body))
      :data))

(defn get-auth-info!
  []
  (-> access-token-url
      (utils/post-ex)
      (parse-resp)))


(defn post-article! [article access-token]
  (let [data (merge article {:access_token access-token})]
    (utils/post-ex post-article-url data)))


(post-article!
  {:title "sdfsdfsdfsdf"
   :content "sdfasdfasdfasdfff"
   :cover_pic "htt://ddfsdfsdf"}
  "1111")