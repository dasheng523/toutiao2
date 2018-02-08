(ns toutiao2.ave40.grap-article
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [toutiao2.ave40.db :refer :all]
            [toutiao2.ave40.utils :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [clojure.tools.logging :as log]
            [clojure.walk :as w]
            [clojure.data]
            [clojure.data.json :as json])
                  (:import (java.io StringReader)))


(def unvisited-urls (ref []))
(def source-htmls (ref []))
(def error_records (atom []))

(defn- add-unvisited [url]
  (if-not (some #{url} @unvisited-urls)
    (alter unvisited-urls conj url)))

(defn- pop-unvisited []
  (when-let [url (peek @unvisited-urls)]
    (alter unvisited-urls pop)
    url))

(defn- add-source-html [url html]
  (alter source-htmls conj {:url url :html html :created_at (quot (System/currentTimeMillis) 1000)}))


(defn- add-error-records [url error_code error_body]
  (swap! error_records conj {:url url :error_code error_code :error_body error_body}))


(defn- is-visited? [url]
  (lazy-contains? (map #(:url %) @source-htmls) url))

(defn- visited [url]
  (let [resp (http/get url)
        body (:body resp)]
    (if
      (= 200 (:status resp))
      body
      (add-error-records url (:status resp) body))))


; 持久化
(defn- add-save-watch []
  (add-watch unvisited-urls :nvu-save-watch
             (fn [_ _ old new]
               (if (< (count new) (count old))                ; 删除记录的情况
                 (if-let [diff (first (clojure.data/diff old new))]
                   (delete-table-data-by-url
                     article-db
                     {:table "unvisited_urls"
                      :urls (remove nil? diff)})))
               (if (> (count new) (count old))                ; 添加记录的情况
                 (if-let [diff (first (clojure.data/diff new old))]
                   (insert-table-tuple
                     article-db
                     {:table "unvisited_urls"
                      :cols ["url"]
                      :datas (map #(conj [] %) (remove nil? diff))})))))

  (add-watch source-htmls :source-htmls-save-watch
             (fn [_ _ old new]
               (if (> (count new) (count old))
                 (if-let [diff (w/stringify-keys (remove nil? (first (clojure.data/diff new old))))]
                   (insert-table-tuple
                     article-db
                     {:table "source_article"
                      :cols (keys (first diff))
                      :datas (map #(vals %) diff)})))))

  (add-watch error_records :error_records-save-watch
             (fn [_ _ old new]
               (if (> (count new) (count old))
                 (if-let [diff (w/stringify-keys (remove nil? (first (clojure.data/diff new old))))]
                   (insert-table-tuple
                     article-db
                     {:table "error_records"
                      :cols (keys (first diff))
                      :datas (map #(vals %) diff)}))))))


(defn- ignore-url [url]
  (let [up-url (str/upper-case url)]
    (or (str/ends-with? up-url ".JPG")
        (str/ends-with? up-url ".PNG")
        (str/ends-with? up-url ".GIF")
        (str/ends-with? up-url ".PDF")
        (str/ends-with? up-url ".XML")
        (str/ends-with? up-url ".MP4")
        (str/ends-with? up-url ".RMVB")
        (not (str/starts-with? url "https://fr.vapingpost.com")))))

(defn- next-url-parser
  "解析需要访问的urls"
  [html]
  (-> html
      (StringReader.)
      (enlive/html-resource)
      (enlive/select [:a])
      ((fn [a-nodes] (map #(-> % :attrs :href) a-nodes)))
      ((fn [urls]
         (remove #(or
                    (nil? %)
                    (ignore-url %))
                 urls)))
      ((fn [urls]
         (map #(-> %
                   (str/split #"#")
                   (first)
                   ((fn [url] (if (str/ends-with? url "/") url (str url "/")))))
              urls)))))

(defn- init-data
  []
  (let [url-list (select-all article-db {:table "unvisited_urls"})
        html-list (select-all article-db {:table "source_article" :cols ["url"] :where "url like 'https://fr.vapingpost.com%'"})]
    (dosync (ref-set unvisited-urls (into [] (map #(:url %) url-list))))
    (dosync (ref-set source-htmls (into [] html-list)))
    (add-save-watch)))


(defn grap-task
  []
  (when-let [url (dosync (pop-unvisited))]
    (try
      (dosync
        (when-not (is-visited? url)
          (log/info (str "visiting: " url))
          (let [html (visited url)]
            (add-source-html url html)
            (when-let [next-urls (next-url-parser html)]
              (doseq [next-url next-urls]
                (add-unvisited next-url))))))
      (catch Exception e
        (log/error e)
        (add-error-records url nil (str e))))))

(def continue (atom true))

(defn do-grap
  []
  (init-data)
  (future (while @continue (grap-task)))
  (future (while @continue (grap-task)))
  (future (while @continue (grap-task)))
  (future (while @continue (grap-task)))
  (future (while @continue (grap-task))))

(defn stop-task []
  (reset! continue false))

(defn restart-task []
  (reset! continue true)
  (do-grap))

(defn first-run-task [url]
  (dosync (add-unvisited url))
  (do-grap))

(defn create-default-selector [select-path domain]
  (fn [resp] (-> resp
                 (StringReader.)
                 (enlive/html-resource)
                 (enlive/select select-path)
                 (->> (map (fn [node]
                             (if domain
                               (str domain (-> node :attrs :href))
                               (-> node :attrs :href))))))))


;; 简易版本抓取
(defn simple-grapper [selector next-page-url-generator]
  (fn [start-page end-page]
    (doseq [page (range start-page end-page)]
      (let [source-url (next-page-url-generator page)
            urls (-> (http/get source-url)
                     :body
                     (selector))]
        (doseq [url urls]
          (println url)
          (if (empty? (select-all article-db {:table "source_article" :where (str "url='" url "'")}))
            (let [html (-> url (http/get) :body)]
              (data-insert!
                "source_article"
                {"url" url "html" html "created_at" (quot (System/currentTimeMillis) 1000)}))))))))


(defn grap-vogue []
  "抓取http://www.vogue.co.uk"
  (let [grapper (simple-grapper
                  (fn [resp] (-> resp
                                 (json/read-str)
                                 (get-in ["data" "template"])
                                 (StringReader.)
                                 (enlive/html-resource)
                                 (enlive/select [:article.c-card :a.c-card__link])
                                 ((fn [a-nodes] (map (fn [node] (str "http://www.vogue.co.uk" (-> node :attrs :href))) a-nodes)))))
                  #(str "http://www.vogue.co.uk/xhr/topic/news?page=" % "&shift=0&list_counter=3"))]
    (grapper 1 1921)))

(defn grap-gamespot []
  "https://www.gamespot.com 被拒绝了"
  (let [grapper (simple-grapper
                  (fn [resp]
                    (-> resp
                        (json/read-str)
                        (get-in ["html"])
                        (StringReader.)
                        (enlive/html-resource)
                        (enlive/select [:article.media-article :a.js-event-tracking])
                        (->> (map #(str "https://www.gamespot.com/" (-> % :attrs :href))))))
                  #(str "https://www.gamespot.com/river/content/popular?page=" %))]
    (grapper 1 75)))

(defn grap-greatist []
  "https://greatist.com"
  (let [grapper (simple-grapper
                  (fn [resp]
                    (-> resp
                        (StringReader.)
                        (enlive/html-resource)
                        (enlive/select [:div.desc-wrapper :div.desc :h2 :a])
                        (->> (map #(-> % :attrs :href)))))
                  #(str "https://greatist.com/recent?page=" %))]
    (grapper 1 300)))

(defn grap-pegfitzpatrick []
  "https://pegfitzpatrick.com/"
  (let [grapper (simple-grapper
                  (fn [resp]
                    (-> resp
                        (StringReader.)
                        (enlive/html-resource)
                        (enlive/select [:article.post :header.entry-header :a.entry-image-link])
                        (->> (map #(-> % :attrs :href)))))
                  #(str "https://pegfitzpatrick.com/page/" % "/"))]
    (grapper 2 60)))


(defn grap-jennstrends []
  "http://www.jennstrends.com"
  (let [grapper (simple-grapper
                  (fn [resp]
                    (-> resp
                        (StringReader.)
                        (enlive/html-resource)
                        (enlive/select [:ul.small-block-grid-1 :li :a.darken])
                        (->> (map #(-> % :attrs :href)))))
                  #(str "http://www.jennstrends.com/blog/page/" % "/"))]
    (grapper 2 40)))



