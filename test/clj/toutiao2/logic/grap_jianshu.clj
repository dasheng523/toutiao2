(ns toutiao.logic.grap-jianshu
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
      (http/get)
      :body))

(defn- parse-article
  "解析文章页"
  [url]
  (let [html (fetch url)
        nodes (utils/to-enlive html)
        selector (partial utils/text-selector nodes)
        page-data (->
                   html
                   (->> (re-find #"page-data\">([\s\S]+?)</script>"))
                   (second)
                   (json/parse-string true))]
    {:title (selector [:div.article :h1.title])
     :author (selector [:div.article :div.author :span.name :a])
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
(-> "https://www.jianshu.com/c/1hjajt?order_by=added_at&page=2"
    parse-article-list
    (->> (map (fn [url]
                (-> (str "https://www.jianshu.com" url)
                    (parse-article))))))

