(ns toutiao2.zimeiti.grap
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as enlive]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.tools.logging :as log]
            [toutiao2.zimeiti.config :as config])
  (:import (java.io StringReader)))


(defn- change-string-to-nodes [s]
  (-> s
      (StringReader.)
      (enlive/html-resource)))

(defn- taojinge-http-get [url]
  (http/get url
            {:headers {"User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.162 Safari/537.36"
                       "Cookie" "UM_distinctid=16262379b3bf9-06e958885e0741-33627805-fa000-16262379b3c5db; _ga=GA1.2.1638181382.1522066357; Hm_lvt_db0fa8577f361832e3b979f920bee38e=1522066355,1522471452,1522482376,1522851065; _gid=GA1.2.1308237853.1522913476; CNZZDATA1261342782=137057632-1522065029-null%7C1523105603; token=3ae0baad98350deedd4591b95ff9b6d03d04c5a1; uid=8174; phone=18926490312; viptime=1524789110"}
             :debug true}))

(defn- fetch-to-enlive [url]
  (-> url
      (http/get {:headers {"User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.162 Safari/537.36"}})
      :body
      (change-string-to-nodes)))

(defn- read-file-enlive [path]
  (-> path
      slurp
      (change-string-to-nodes)))

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


(defn parse-info [node]
  (let [link-selector (create-default-text-selector [:figure [:a (enlive/nth-child 3)]])
        name-selector (create-default-text-selector [:figure [:a (enlive/nth-child 1)]])
        desc-selector (create-default-text-selector [:figure :figcaption])]
    {:link (str/trim (link-selector node))
     :title (str/trim (name-selector node))
     :desc (str/trim (desc-selector node))}))

(defn fetch-atlas-pic [content]
  (-> content
      (->> (re-find #"sub_images\\\":([\s\S]+?),\\\"max_img_width"))
      second
      (str/replace #"\\" "")
      (json/parse-string true)
      (->> (map :url))))

(defn retry-fetch
  ([url times]
   (if (> times 3)
     nil
     (try
       (http/get url
                 {:headers {"User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.162 Safari/537.36"}})
       (catch Exception e
         (retry-fetch url (+ times 1))))))
  ([url]
   (retry-fetch url 0)))

(defn retry-download
  ([url times]
   (if (> times 3)
     nil
     (try
       (http/get url
                 {:as :byte-array
                  :headers {"User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.162 Safari/537.36"}})
       (catch Exception e
         (println (str "fail times " times))
         (retry-fetch url (+ times 1))))))
  ([url]
   (retry-download url 0)))




(defn download-file [uri file]
  (some-> (retry-download uri)
          :body
          (io/copy (io/file file)))
  #_(with-open [in (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out)))

(defn- generate-filename
  [suffix]
  (str (quot (System/currentTimeMillis) 1000) (rand-int 1000) suffix))

(defn- time-base-dir [base]
  (let [cformat (tf/formatter "yyyyMMdd")
        timestr (tf/unparse cformat (t/now))
        path (str base "/" timestr "/")]
    path))


(defn- download-toutiao-piture [url]
  (let [filename (str (time-base-dir (config/get-download-path)) (generate-filename ".png"))]
    (io/make-parents filename)
    (println (str "downloading image: " url))
    (try
      (download-file url filename)
      filename
      (catch Exception e
        (log/error e)))))


(defn change-pic-md5 [pic-path]
  (with-open [w (io/writer pic-path :append true)]
    (.write w (rand-int 100000)))
  pic-path)


(defn product-item-info [url]
  (let [node-tree (-> url
                      taojinge-http-get
                      :body
                      (change-string-to-nodes))
        title-selector (create-default-text-selector [:div.tit :h1])
        toutiao-selector (create-default-href-selector [[:div.container (enlive/nth-child 1)] :span :a])
        title (-> node-tree title-selector)
        atlas-url (-> node-tree toutiao-selector)
        figure-list (-> node-tree
                        (enlive/select [:figure])
                        (->> (map parse-info)))
        pic-list (-> (fetch-atlas-pic (:body (retry-fetch atlas-url)))
                     (->> (map #(download-toutiao-piture %)))
                     (->> (map change-pic-md5)))
        goods-list (map #(conj %1 {:pic %2}) figure-list pic-list)]
    {:atitle title :goods goods-list}))

#_(let [url "http://www.51taojinge.com/jinri/temai_content_article.php?id=1170322&check_id=2"
      node-tree (-> url
                    taojinge-http-get
                    :body
                    (change-string-to-nodes))
      title-selector (create-default-text-selector [:div.tit :h1])
      toutiao-selector (create-default-href-selector [[:div.container (enlive/nth-child 1)] :span :a])
      title (-> node-tree title-selector)
      atlas-url (-> node-tree toutiao-selector)
      figure-list (-> node-tree
                      (enlive/select [:figure])
                      (->> (map parse-info)))
      pic-list (-> (fetch-atlas-pic (:body (retry-fetch atlas-url)))
                   (->> (map #(download-toutiao-piture %)))
                   (->> (map change-pic-md5)))
      goods-list (map #(conj %1 {:pic %2}) figure-list pic-list)]
  {:atitle title :goods goods-list})


#_(taojinge-http-get "http://www.51taojinge.com/jinri/temai_content_article.php?id=1170322&check_id=2")
