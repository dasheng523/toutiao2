(ns toutiao2.tuding
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clojure.spec.alpha :as s]
            [slingshot.slingshot :as sli]
            [clojure.tools.logging :as log]))
(s/check-asserts true)

(def headers
  {"Accept" "application/json"
   "Accept-Language" "zh-CN,zh;q=0.8"
   "User-Agent" "Mozilla/5.0 (Linux; U; Android 4.4.4; zh-cn; CHM-TL00H Build/tt) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"
   "X-Live-Session-Token" "1.16135003.988853.umW.1de185d85fbe349d2b0a01f937796df6"
   "Content-Type" "application/json"
   "X-Live-App-Version" "1.0.7"
   "X-Live-Device-Type" "android"
   "Accept-Encoding" "gzip"})


(defn query [url params]
  (-> (http/post
        url
        {:headers headers
         :proxy-host "127.0.0.1"
         :proxy-port 55555
         :body (json/generate-string params)})
      :body))

(defn query-get [url]
  (-> (http/get
        url
        {:headers headers
         :proxy-host "127.0.0.1"
         :proxy-port 55555})
      :body))


(s/def :resp/code #(= % 0))
(s/def :resp/msg #(= "请求成功" %))
(s/def ::resp-rank (s/keys :req-un [:resp/code :resp/msg]))
(defn rank []
  (-> (query "http://api.api.chongdingdahui.com/win/weeklyRankings" {})
      (json/parse-string true)
      (->> (s/assert ::resp-rank))))


(s/def :data/countdown int?)
(s/def :data/liveId int?)
(s/def :liveno/data (s/keys :req-un [:data/countdown :data/liveId]))
(s/def ::resp-liveno (s/keys :req-un [:resp/code :resp/msg :liveno/data]))
(defn liveno []
  (-> (query "http://api.api.chongdingdahui.com/live/now" {})
      (json/parse-string true)
      (->> (s/assert ::resp-liveno))))


(s/def :event/desc string?)
(s/def :event/options (s/coll-of string?))
(s/def :event/questionId int?)
(s/def :data/event (s/keys :req-un [:event/desc :event/options :event/questionId]))
(s/def :data/type #(= % "showQuestion"))
(s/def :question/code #(= % 0))
(s/def :question/data (s/keys :req-un [:data/event :data/type]))
(s/def ::question-spec (s/keys :req-un [:question/code :question/data]))
(defn get-question []
  (let [resp (query-get "http://msg.api.chongdingdahui.com/msg/current")]
    (if (= resp "not found")
      (do (log/error "question not found")
          (sli/throw+ {}))
      (json/parse-string true))))

(defn parse-question [question-data]
  (s/assert ::question-spec question-data)
  {:title (get question-data [:data :event :desc])
   :option (get question-data [:data :event :options])
   :id (get question-data [:data :event :questionId])})

(defn do-answer [quesion-id option-index]
  (query "http://answer.api.chongdingdahui.com/answer/do" {:questionId quesion-id :option option-index}))


(defn find-answer [question-str]
  (-> (str "https://www.google.com.hk/search?q=" question-str)
      (query-get)
      :body))

(query-get "https://www.baidu.com")

(find-answer "9.动画片《海绵宝宝》中，痞老板最害怕的是？")

#_(s/assert ::question-spec {:code 0,
                           :msg "成功",
                           :data {:event {:desc "9.动画片《海绵宝宝》中，痞老板最害怕的是？",
                                          :type "showQuestion",
                                          :showTime 1515733939449,
                                          :status 0,
                                          :displayOrder 8,
                                          :liveId 98,
                                          :options ["鲸鱼","蟹黄堡","海星"],
                                          :questionId 1182,
                                          :answerTime 10},
                                  :type "showQuestion"}})
