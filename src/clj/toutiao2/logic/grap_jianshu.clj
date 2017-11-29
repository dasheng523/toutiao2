(ns toutiao.logic.grap-jianshu
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as enlive])
  (:import (java.io StringReader)))

;; Common
(defn fetch [url]
  (-> url
      (http/get)
      :body))

(defn to-enlive [html]
  (-> html
      (StringReader.)
      (enlive/html-resource)))

(defn create-default-text-selector [node-select]
  (fn [nodes]
    (str/join
     "\n"
     (map (fn [n]
            (enlive/text n))
          (enlive/select nodes node-select)))))

(defn create-default-href-selector [node-select]
  (fn [nodes]
    (let [find-rs (enlive/select nodes node-select)]
      (if-not (empty? find-rs)
        (-> find-rs first :attrs :href)))))

;; Parse
(defn- get-counts [s]
  (-> s
      (str/trim)
      (str/split #" ")
      (last)))

(defn- parse-article
  "解析文章页"
  [url]
  (let [html (fetch url)
        nodes (to-enlive html)
        title-selector (create-default-text-selector
                        [:div.article :h1.title])
        author-selector (create-default-text-selector
                         [:div.article :div.author :span.name :a])
        author-desc-selector (create-default-text-selector
                              [:div.follow-detail :div.info :p])
        views-selector (create-default-text-selector
                        [:span.views-count])
        comments-selector (create-default-text-selector
                           [:span.comments-count])
        likes-selector (create-default-text-selector
                        [:span.likes-count])
        page-data (->
                   html
                   (->> (re-find #"page-data\">([\s\S]+?)</script>"))
                   (second)
                   (json/parse-string true))]
    {:title (title-selector nodes)
     :author (author-selector nodes)
     :followers_count (get-in page-data [:note :author :followers_count])
     :total_likes_count (get-in page-data [:note :author :total_likes_count])
     :view_count (get-in page-data [:note :views_count])
     :wordage (get-in page-data [:note :public_wordage])
     :comment_count (get-in page-data [:note :comments_count])
     :url url}))

(defn- find-article-urls
  "解析列表页链接"
  [url]
  (-> url
      fetch
      to-enlive
      (enlive/select [:ul.note-list :div.content :a.title])
      (->> (map #(str "http://www.jianshu.com" (-> % :attrs :href))))))



;; Test
#_(-> "http://www.jianshu.com/c/1hjajt?order_by=added_at&page=2"
    find-article-urls
    (->> (map (fn [url]
                (-> url
                    (parse-article))))))

