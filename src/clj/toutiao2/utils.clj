(ns toutiao2.utils
  (:require [clojure.spec.alpha :as s]
            [clj-http.client :as http]))


(s/def :http-response/status (s/and int? #(= 200 %)))
(s/def :http-response/body string?)
(s/def ::http-response (s/keys :req-un [:http-response/status :http-response/body]))

(defn post-ex
  ([url]
   (post-ex url nil))
  ([url params]
    (-> url
        (http/post params)
        (->> (s/assert ::http-response))
        :body)))


(defn get-ex
  [url]
  (-> url
      (http/get)
      (->> (s/assert ::http-response))
      :body))