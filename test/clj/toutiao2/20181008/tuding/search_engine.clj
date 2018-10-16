(ns toutiao2.tuding.search-engine
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as enlive]
            [toutiao2.utils :as utils])
  (:import (java.io StringReader)))


;;;;;;;;;;;;;;;;;;;;;;;  搜索  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def search-request
  {:headers {"Accept-Encoding" "gzip, deflate"
             "Accept-Language" "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2"
             "content-type" "text/html; charset=utf-8"
             "User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36"}})

(def baidu-base-url "https://www.baidu.com/s?wd=")
(def biying-base-url "https://cn.bing.com/search?q=")
(def google-base-url "https://www.google.co.jp/search?q=")
(def so-base-url "https://www.so.com/s?ie=utf-8&fr=none&src=360sou_newhome&q=")

(defn get-html-text [html selector]
  (-> (utils/to-enlive html)
      (enlive/select selector)
      (enlive/texts)
      (first)))

(defn- create-search-engine [search-base-url request content-selector]
  (fn [question]
    (-> (str search-base-url question)
        (http/get request)
        :body
        (get-html-text content-selector))))

(def google-search (create-search-engine google-base-url
                                         (assoc search-request
                                                :proxy-host "127.0.0.1"
                                                :proxy-port 50461)
                                         [:div#rcnt :div#center_col :div#search]))
(def baidu-search (create-search-engine baidu-base-url search-request [:div#content_left]))
(def biying-search (create-search-engine biying-base-url search-request [:div#b_content]))
(def so-search (create-search-engine so-base-url search-request [:div#main]))
