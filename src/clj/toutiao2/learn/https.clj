(ns toutiao2.learn.https
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clj-bosonnlp.core :as bos]
            [net.cgrand.enlive-html :as enlive])
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


(defn to-enlive [html]
  (-> html
      (StringReader.)
      (enlive/html-resource)))

(defn get-html-text [html selector]
  (-> (to-enlive html)
      (enlive/select selector)
      (enlive/texts)
      (first)))

(defn- google-search [question]
  (-> (str "https://www.google.co.jp/search?q=" question)
      (http/get {:headers {"Accept-Encoding" "gzip, deflate"
                           "Accept-Language" "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2"
                           "content-type" "text/html; charset=utf-8"}
                 :as "gbk"})
      :body
      (get-html-text [:body])))


(defn- baidu-search [question]
  (-> (str "https://www.baidu.com/s?wd=" question)
      (http/get {:headers {"Accept-Encoding" "gzip, deflate"
                           "Accept-Language" "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2"
                           "content-type" "text/html; charset=utf-8"
                           "User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36"}})
      :body
      (get-html-text [:body])))

(defn- biying-search [question]
  (-> (str "https://cn.bing.com/search?q=" question)
      (http/get {:headers {"Accept-Encoding" "gzip, deflate"
                           "Accept-Language" "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2"
                           "content-type" "text/html; charset=utf-8"
                           "User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36"}})
      :body
      (get-html-text [:body])))


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

(def default-proxy-config
  {:proxy-host "127.0.0.1"
   :proxy-port 50461
   :socket-timeout 3000 :conn-timeout 3000})

(def default-params
  (merge {:headers default-headers}
         default-proxy-config))

(defn- merge-token [params token]
  (update-in params
             [:headers]
             assoc "X-Live-Session-Token" token))


(defn- merge-body [params body]
  (assoc params :body body))

(defn- create-query [method headers]
  (fn [url params]
    (method url
            (merge params
                   {:headers headers
                    :proxy-host "127.0.0.1"
                    :proxy-port 50461}))))


(defn- do-answer [token question-id option-index]
  (http/post (str domain "/answer/do")
             (-> default-params
                 (merge-token token)
                 (merge-body (json/generate-string
                              {:questionId question-id :option option-index})))))

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
                (-> default-params
                    (merge-token token)))
      :body))

(defn- get-test-question [file]
  (-> file
      slurp))



(defn- do-logic []
  (try
    (println "============ starting =========")
    (let [content
          (get-test-question "/Users/huangyesheng/Downloads/16.txt")
          #_(get-question "1.16135003.1082161.kas.6611d5742e6111dfe51bf114f6b38343")]
      (println (str "content: " content))
      (let [{:keys [question options] :as question-info}
            (parse-question-info content)]
        (when question
          (println question)

          (future (println "google:"
                           (compute-percent
                            (find-question question options google-search))))
          (future (println "biying"
                           (compute-percent
                            (find-question question options biying-search))))
          (future (println "baidu"
                           (compute-percent
                            (find-question question options baidu-search))))

          (when (some #(re-find #"\d" %) options)
            (future (println (compute-percent (find-question question
                                                             (map number-synonym options)
                                                             google-search))))
            (future (println (compute-percent (find-question question
                                                             (map number-synonym options)
                                                             biying-search))))
            (future (println (compute-percent (find-question question
                                                             (map number-synonym options)
                                                             baidu-search))))
            ))))
    (catch Exception e
      (log/error (.getMessage e)))))

(do-logic)




(bos/initialize "wcY5KVg5.21955.F8s57q6YTp36")

(bos/depparser "邓紫棋谈男友林宥嘉：我觉得我比他唱得好")





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

#_(-> (str domain "/msg/current")
    (query-get {})
    :body
    (parse-question-info))

#_(-> "/Users/huangyesheng/Downloads/11.txt"
    slurp
    (parse-question-info))




