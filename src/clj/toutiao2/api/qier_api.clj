(ns toutiao2.api
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.spec.alpha :as s]))

(s/check-asserts true)

(def client-id "524ed579c87ddb2dcd0bfd197d321547")
(def client-secret "515def9db75e46f9fe9ee2c815d93b46ca1919fb")
(def access-token-url (str "https://auth.om.qq.com/omoauth2/accesstoken?grant_type=clientcredentials&client_id=" client-id "&client_secret=" client-secret))


(s/def ::status (s/and int? #(= 200 %)))
(s/def ::body string?)
(s/def ::http-response (s/keys :req-un [::status ::body]))


(s/assert ::http-response {:status 200 :body "dfdfdfd"})

(-> access-token-url
    (http/post)
    (->> (s/assert ::http-response))
    :body
    (json/parse-string true))
