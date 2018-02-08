(ns toutiao2.ave40.grap-image
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [toutiao2.ave40.db :refer :all]
            [toutiao2.ave40.utils :as utils]
            [clojure.data.json :as json]
            [net.cgrand.enlive-html :as enlive])
  (:import (java.io StringReader)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;; 读取所有图片，并将图片push到服务器上 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn fecth-article-images-and-save
  "读取文章表的图片，并将图片保存到图片表里面，最后更新文章的图片字段"
  []
  (let [list (select-all article-db {:table "articles" :cols ["id" "extra"] :where "NOT ISNULL(extra) AND extra<>''"})]
    (doseq [item list]
      (if (.startsWith (:extra item) "http")
        (-> (-> item :extra (str/split #"\n"))
            (->> (map #(-> (data-insert! "image" {"source_url" %}) :generated_key)))
            ((fn [n] {:images n}))
            (json/write-str)
            (#(update-data article-db {:table "articles" :updates {:extra %} :where (str "id=" (:id item))})))))))

(defn- push-image [url]
  (-> (http/post "http://manage.ecigview.com/file/saveFile" {:form-params {:filesrc url}})
      :body))

(defn push-and-save-images
  "将image表的图片通通push到服务器，然后保存服务器图片地址"
  []
  (let [list (select-all article-db {:table "image" :where "isnull(url)"})]
    (doseq [item list]
      (let [url (push-image (:source_url item))]
        (println url)
        (update-data article-db {:table "image" :updates {:url url} :where (str "id=" (:id item))})))))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;; photosforclass 图片的抓取;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- parse-image-url
  "将HTML里面的图片地址提取出来"
  [html]
  (let [nodes (->
                html
                (StringReader.)
                (enlive/html-resource))
        url-nodes (enlive/select nodes [:div.flicr-photo :> :a])]
    (doseq [node url-nodes]
            (let [url (str "http://www.photosforclass.com" (-> node :attrs :href))
                  filename (-> node :attrs :download)]
              (data-insert! "image" {"source_url" url "filename" filename})))))

(defn- fetch-page [n]
  (let [url (str "http://www.photosforclass.com/search/vape/" n)]
    (println url)
    (parse-image-url (:body (http/get url)))))

(defn- do-fetch-all []
  (doseq [n (range 63)]
    (fetch-page (+ n 1))))

