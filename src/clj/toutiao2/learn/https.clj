(ns toutiao2.learn.https
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as enlive]
            [toutiao2.tuding.search-engine :as search-engine])
  (:import (java.io StringReader)))

(def number-map
  {"1" "一"
   "2" "二"
   "3" "三"
   "4" "四"
   "5" "五"
   "6" "六"
   "7" "七"
   "8" "八"
   "9" "九"
   "0" "零"})

(defn number-synonym [s]
  (let [ks (keys number-map)]
    (reduce (fn [stemp k]
              (str/replace stemp (re-pattern k) (get number-map k)))
            s ks)))


(defn- account-keyword [html keys]
  (reduce #(assoc %1
                  %2
                  (count (re-seq (re-pattern %2) html)))
          {}
          keys))

(defn- compute-percent [m]
  (let [total (reduce + (vals m))
        ks (keys m)]
    (reduce (fn [cls k] (update cls k #(format "%.2f" (float (* (/ %1 %2) 100))) total))
            m
            ks)))


(defn find-question [question options search-method]
  (-> (search-method question)
      (account-keyword options)))



(def domain "http://msg.api.chongdingdahui.com")
(def default-headers
  {"Accept-Language" "zh-CN,zh;q=0.8"
   "User-Agent" "Mozilla/5.0 (Linux; U; Android 7.0; zh-cn; MI 5 Build/NRD90M) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"
   "X-Live-App-Version" "1.0.7"
   "X-Live-Device-Type" "android"
   "Accept-Encoding" "gzip"})

(def default-http-config
  {:socket-timeout 3000
   :conn-timeout 3000})

(def default-params
  (merge {:headers default-headers}
         default-http-config))

(defn- merge-token [params token]
  (update-in params
             [:headers]
             assoc "X-Live-Session-Token" token))

(defn- merge-body [params body]
  (assoc params :body body))

(defn- parse-question-info [text]
  (let [data (json/parse-string text true)]
    {:question (-> (get-in data [:data :event :desc])
                   (str/trim)
                   (str/split #"\." 2)
                   (#(if (> (count %) 1) (second %) (first %))))
     :options (-> data
                  (get-in [:data :event :options])
                  (json/parse-string true)
                  (->> (map #(str/trim %))))}))

(defn- get-question [token]
  (-> (http/get (str domain "/msg/current")
                (-> default-params (merge-token token)))
      :body))


(defn- get-test-question [file]
  (-> file
      slurp))


(defn- do-logic []
  (try
    (println "============ starting =========")
    (let [content
          #_(get-test-question "d:/16.txt")
          (get-question "1.16135003.1082161.kas.6611d5742e6111dfe51bf114f6b38343")]
      (println (str "content: " content))
      (let [{:keys [question options] :as question-info}
            (parse-question-info content)]
        (when question
          (println question)

          #_(future (println "google:"
                           (compute-percent
                            (find-question question options search-engine/google-search))))
          (future (println "biying"
                           (compute-percent
                            (find-question question options search-engine/biying-search))))
          (future (println "baidu"
                           (compute-percent
                            (find-question question options search-engine/baidu-search))))
          (future (println "soso"
                           (compute-percent
                             (find-question question options search-engine/so-search))))

          (when (some #(re-find #"\d" %) options)
            #_(future (println (compute-percent (find-question question
                                                             (map number-synonym options)
                                                             search-engine/google-search))))
            (future (println (compute-percent (find-question question
                                                             (map number-synonym options)
                                                             search-engine/biying-search))))
            (future (println (compute-percent (find-question question
                                                             (map number-synonym options)
                                                             search-engine/baidu-search))))))))
    (catch Exception e
      (log/error (.getMessage e)))))

(do-logic)




(defn ddd []
  (-> (http/post (str domain "/barrage/send")
                (-> default-params
                    (merge-token "1.16135003.1082161.kas.6611d5742e6111dfe51bf114f6b38343")
                    (merge-body (json/generate-string {"text" "aaaaaaaaa"
                                                       "type" "1"
                                                       "liveId" "18363"}))))
      :body))

(ddd)


(-> default-params
    (merge-token "1.16135003.1082161.kas.6611d5742e6111dfe51bf114f6b38343")
    (merge-body (json/generate-string {"text" "aaaaaaaaa"
                                       "type" "1"
                                       "liveId" "18363"})))



