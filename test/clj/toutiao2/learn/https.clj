(ns toutiao2.learn.https
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as enlive]
            [toutiao2.tuding.search-engine :as search-engine])
  (:import (java.io StringReader)
           (com.hankcs.hanlp HanLP)))

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



(defn match-account
  "统计两个字符串的匹配数"
  [source target]
  (let [source-keys (HanLP/extractKeyword source 1000)
        target-keys (HanLP/extractKeyword target 1000)]
    (account-keyword source (filter (set target-keys) source-keys))))

(defn result-match-account [m]
  (str (count m) "-" (reduce + (vals m))))

(defn search-targets [source targets]
  (map #(identity {% ((comp result-match-account
                            (partial match-account source)) %)})
       targets))

(defn account-options [question options search]
  (for [o options]
    {o (match-account (search o) question)}))

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
          #_(get-test-question "/Users/huangyesheng/Downloads/16.txt")
          (get-question "1.16135003.1082161.kas.6611d5742e6111dfe51bf114f6b38343")]
      (println (str "content: " content))
      (let [{:keys [question options] :as question-info}
            (parse-question-info content)
            rs-fun #(search-targets (% question) options)]
        (when question
          (println question)
          (println options)

          (future (println "google:"
                           (rs-fun search-engine/google-search)))
          (future (println "biying"
                           (rs-fun search-engine/biying-search)))
          (future (println "baidu"
                           (rs-fun search-engine/baidu-search)))

          (future (println (account-options question options search-engine/google-search)))
          #_(future (println "biying" (account-options question options search-engine/biying-search)))
          #_(future (println "baidu" (account-options question options search-engine/baidu-search)))
          )))
    (catch Exception e
      (log/error (.getMessage e)))))



(do-logic)




(defn search-targets [source targets]
  (map #(identity {% ((comp result-match-account
                            (partial match-account source)) %)})
       targets))

(defn account-options [question options search]
  (doseq [o options]
    (println {o (match-account (search o) question)})))

(sss "AlphaGo没有战胜过以下哪位选手?"
     ["柯洁" "周睿羊" "许银川"]
     search-engine/baidu-search)

(search-targets (search-engine/baidu-search "AlphaGo没有战胜过以下哪位选手?")
                ["柯洁" "周睿羊" "许银川"])


(match-account (search-engine/biying-search "全球首款人工智能手机芯片是?")
               "麒麟950芯片")
(match-account (search-engine/biying-search "全球首款人工智能手机芯片是?")
               "麒麟960芯片")
(-> (match-account (search-engine/google-search "周睿羊")
                   "AlphaGo没有战胜过以下哪位选手")
    result-match-account)
(-> (match-account (search-engine/google-search "《生化危机6》")
                   "以下哪部电影中出现了超级计算机“红后”？")
    result-match-account)

(HanLP/extractKeyword "以下哪部电影中出现了超级计算机“红后”？" 100)


(Def ss "/cdn/h/1/comment/brow/13241410?iid=23704761247&device_id=21101274775&ac=wifi&channel=xiaomi&aid=1248&app_name=fantasy&version_code=601&version_name=6.0.1&device_platform=android&ssmix=a&device_type=MI+5&device_brand=Xiaomi&language=zh&os_api=24&os_version=7.0&openudid=4112f307c9f2f01c&manifest_version_code=101&resolution=1080*1920&dpi=480&update_version_code=6010&_rticket=1516285394581")

  (Def ss2 "/cdn/h/1/comment/brow/13241411?iid=23704761247&device_id=21101274775&ac=wifi&channel=xiaomi&aid=1248&app_name=fantasy&version_code=601&version_name=6.0.1&device_platform=android&ssmix=a&device_type=MI+5&device_brand=Xiaomi&language=zh&os_api=24&os_version=7.0&openudid=4112f307c9f2f01c&manifest_version_code=101&resolution=1080*1920&dpi=480&update_version_code=6010&_rticket=1516286787009")

(def ss-url (str "http://api-spe-ttl.ixigua.com" ss2))
(println (http/get ss-url {:as "utf-8"}))



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



