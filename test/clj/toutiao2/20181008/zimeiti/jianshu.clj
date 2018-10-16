(ns toutiao2.zimeiti.jianshu
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as enlive]
            [toutiao2.utils :as utils])
  (:import (java.io StringReader)))

;; Common
(defn fetch [url]
  (-> url
      (http/get {:headers {"User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.162 Safari/537.36"
                           "Cookie" "UM_distinctid=16262379b3bf9-06e958885e0741-33627805-fa000-16262379b3c5db; _ga=GA1.2.1638181382.1522066357; Hm_lvt_db0fa8577f361832e3b979f920bee38e=1522066355,1522471452,1522482376,1522851065; _gid=GA1.2.1308237853.1522913476; CNZZDATA1261342782=137057632-1522065029-null%7C1523105603; token=3ae0baad98350deedd4591b95ff9b6d03d04c5a1; uid=8174; phone=18926490312; viptime=1524789110"}
                 :debug true})
      :body))

(defn parse-article
  "解析文章页"
  [url]
  (let [html (fetch url)
        nodes (utils/to-enlive html)
        selector (partial utils/text-selector nodes)
        html-selector (partial utils/html-selector nodes)
        page-data (->
                   html
                   (->> (re-find #"page-data\">([\s\S]+?)</script>"))
                   (second)
                   (json/parse-string true))]
    {:title (selector [:div.article :h1.title])
     :author (selector [:div.article :div.author :span.name :a])
     :content (html-selector [:div.article :div.show-content])
     :followers_count (get-in page-data [:note :author :followers_count])
     :total_likes_count (get-in page-data [:note :author :total_likes_count])
     :view_count (get-in page-data [:note :views_count])
     :wordage (get-in page-data [:note :public_wordage])
     :comment_count (get-in page-data [:note :comments_count])
     :url url}))


(defn- parse-article-list
  "解析列表页链接"
  [url]
  (-> url
      fetch
      utils/to-enlive
      (utils/hrefs-selector [:ul.note-list :div.content :a.title])))


;; Test

