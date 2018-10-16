(ns toutiao2.html-parser
  (:require [toutiao2.utils :as utils]
            [net.cgrand.enlive-html :as enlive]
            [cheshire.core :as json]
            [clojure.string :as str]
            [toutiao2.http :as http])
  (:import (java.io StringReader)))

(defn to-enlive [html]
  (-> html
      (StringReader.)
      (enlive/html-resource)))

(defn- text-selector
  [nodes node-select]
  (str/join
   "\n"
   (map (fn [n]
          (enlive/text n))
        (enlive/select nodes node-select))))

(defn- html-selector
  [nodes node-select]
  (when-let [find-rs (enlive/select nodes node-select)]
    (-> find-rs
        (enlive/emit*)
        (str/join))))

(defn- attribute-selector
  "取某个元素中的属性 attr是关键词 :href :src等"
  [nodes node-select attr]
  (let [find-rs (enlive/select nodes node-select)]
    (if-not (empty? find-rs)
      (-> find-rs first :attrs attr))))

(defn- attributes-selector
  "取多个元素中的属性"
  [nodes node-select attr]
  (let [find-rs (enlive/select nodes node-select)]
    (if find-rs
      (map #(-> % :attrs attr) find-rs))))



(defn parse
  "根据特定的选择器，解析出html中的内容"
  [html selectors]
  (let [nodes (to-enlive html)]
    (reduce (fn [cl item]
              (assoc cl (first item)
                     ((last item) nodes))) {} selectors)))

(defn select-text
  [rules]
  #(text-selector % rules))

(defn select-html
  [rules]
  #(html-selector % rules))

(defn select-href
  [rules]
  #(attribute-selector % rules :href))

(defn select-hrefs
  [rules]
  #(attributes-selector % rules :href))

(defn select-imgsrc
  [rules]
  #(attribute-selector % rules :src))

(defn select-json
  [regex]
  #(->
    %
    (enlive/emit*)
    (str/join)
    (->> (re-find regex))
    (second)
    (json/parse-string true)))

#_(parse (-> (http/do-get "https://www.jianshu.com/p/0a88fdd37b39" nil)
           :body)
       {:title (select-text [:div.article :h1.title])
        :author (select-text [:div.article :div.author :span.name :a])
        :author-url (select-href [:div.article :div.author :a.avatar])
        :content (select-html [:div.article :div.show-content])
        :extra (select-json #"page-data\">([\s\S]+?)</script>")
        })
