(ns toutiao2.zimeiti.qier
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [toutiao2.utils :as utils]
            [toutiao2.zimeiti.jianshu :as jianshu]))

(s/check-asserts true)
(s/def :body/code (s/and string? #(= "0" %)))
(s/def :data/access_token string?)
(s/def :data/expires_in int?)
(s/def :data/openid string?)
(s/def :body/data (s/keys :req-un [:data/access_token :data/expires_in :data/openid]))
(s/def ::body (s/keys :req-un [:body/code :body/data]))


(def client-id "524ed579c87ddb2dcd0bfd197d321547")
(def client-secret "515def9db75e46f9fe9ee2c815d93b46ca1919fb")
(def access-token-url (str "https://auth.om.qq.com/omoauth2/accesstoken?grant_type=clientcredentials&client_id=" client-id "&client_secret=" client-secret))
(def post-article-url "https://api.om.qq.com/article/clientpubpic")
(def token-info (atom []))

(defn reset-token! [token-data]
  (reset! token-info (assoc token-data :ctime (System/currentTimeMillis))))


(defn parse-resp
  [resp]
  (-> resp
      (json/parse-string true)
      (->> (s/assert ::body))
      :data))

(defn get-auth-info!
  []
  (-> access-token-url
      (utils/post-ex)
      (parse-resp)))

(defn current-token!
  "获取token信息
  目前没辙，只能这样写了。"
  []
  (let [now (System/currentTimeMillis)
        ctime (:ctime @token-info)
        expire (:expires_in @token-info)]
    (if (or (empty? @token-info) (> (/ (- now ctime) 1000) expire))
      (-> (get-auth-info!)
          (reset-token!)))
    (:access_token @token-info)))


(defn post-article!
  "发布文章"
  [article access-token]
  (let [data (merge article {:access_token access-token})]
    (utils/post-ex post-article-url data)))

#_(def data (jianshu/parse-article "https://www.jianshu.com/p/7cb27667442a"))


#_(post-article!
 {:title (str/replace (:title data) #"｜" " ")
  :content (:content data)
  :cover_pic "https://upload-images.jianshu.io/upload_images/4702070-dc5e39630a2dcb88.jpeg?imageMogr2/auto-orient/strip%7CimageView2/2/w/640"}
 (current-token!))


#_(post-article!
    {:title "test title"
     :content "<p>test content</p><img src=\"https://dn-phphub.qbox.me/uploads/images/201612/10/1/k7wwMpJduq.jpg\">"
     :cover_pic "https://dn-phphub.qbox.me/uploads/images/201612/10/1/k7wwMpJduq.jpg"}
    (current-token!))

(defn post-media!
  "发布视频"
  [media access-token]
  (let [data (merge media {:access_token access-token})]
    (utils/post-multipart-ex post-article-url data)))

#_(post-media!
  {:title "testmedia"
   :tags "篮球"
   :cat 100
   :desc "test desc"
   :md5 "1111"
   :media (clojure.java.io/file "f:/h0513j6olou.mp4")}
  (current-token!))
