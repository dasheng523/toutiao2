(ns toutiao2.ave40.article
  (:require [clj-http.client :as http]
            [clj-http.util :as http-util]
            [toutiao2.ave40.db :refer :all]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as enlive]
            [clojure.walk :as w]
            [clojure.data.json :as json])
  (:import (java.io StringReader)))

(defn- export-to-cvs
  [list]
  (spit "d:/sss.csv"
        (str/join "\n"
                  (map (fn [n]
                         (str/join "," (map (fn [[k v]] (str "\"" v "\"")) n)))
                       list))))

(defn create-default-text-selector [node-select]
  (fn [nodes]
    (str/join
      "\n"
      (map (fn [n]
             (enlive/text n))
           (enlive/select nodes node-select)))))

(defn create-default-image-selector [node-select]
  (fn [nodes]
    (-> nodes
        (enlive/select node-select)
        (#(map (fn [n] (-> n :attrs :data-src)) %))
        (->> (str/join "\n"))
        #_(json/write-str))))

(defn create-parser [handlers]
  "从html提取信息,selectors是一个列表[{:name :title :selector #()}]"
  (fn [html]
    (reduce
      (fn [coll {:keys [name selector]}]
        (let [html-nodes (-> html
                             (StringReader.)
                             (enlive/html-resource))]
          (conj coll {name (selector html-nodes)})))
      {}
      handlers)
    #_(for [{:keys [name selector]} handlers]
      (let [html-nodes (-> html
                           (StringReader.)
                           (enlive/html-resource))]
        {name (selector html-nodes)}))))



(defn do-parse-and-save [{:keys [domain handlers cond]}]
  (let [found-list (select-all article-db
                                {:table "source_article"
                                 :where (str "url like '" domain "%' and " cond)
                                 :cols ["id" "url"]})
        parser (create-parser handlers)]
    (println (str "total:" (count found-list)))
    (doseq [finfo found-list]
      (when (empty? (select-all article-db {:table "articles" :where (str "source_url='" (:url finfo) "'")}))
        (let [source (select-one article-db {:table "source_article" :where (str "id=" (:id finfo))})]
          (println (:url source))
          (data-insert! "articles"
                        (w/stringify-keys
                          (merge (parser (:html source))
                                 {:source_url (:url source)
                                  :grap_time (:created_at source)}))))))))


(defn parse-vogue []
  "解析http://www.vogue.co.uk的文章内容和图片"
  (do-parse-and-save {:domain "http://www.vogue.co.uk"
                      :handlers [{:name :title,
                                  :selector (create-default-text-selector
                                              [:div.a-header__content :> :h1.a-header__title])}
                                 {:name :article
                                  :selector (create-default-text-selector
                                              [:div.a-body__content :> #{:p :h2}])}
                                 {:name :extra
                                  :selector (create-default-image-selector
                                              [:div.a-body__content :figure :div.bb-figure__wrapper :> :img])}]
                      :cond "html like '%a-header__title%' and html like '%a-body__content%'"}))


(defn parse-autoexpress []
  "解析http://www.autoexpress.co.uk的文章内容和图片"
  (do-parse-and-save {:domain "http://www.autoexpress.co.uk"
                      :handlers [{:name :title,
                                  :selector (create-default-text-selector
                                              [:div.content :> :div.title-group-inline :h1.title])}
                                 {:name :article
                                  :selector (create-default-text-selector
                                              [:div.content :> :div.field-name-body :p])}
                                 {:name :extra
                                  :selector (create-default-image-selector
                                              [:div.content :> :figure :div.primary-image :> :img])}]
                      :cond "html like '%title-group-inline%' and html like '%field field-name-body%'"}))


(defn parse-greatist
  "https://greatist.com"
  []
  (do-parse-and-save {:domain "https://greatist.com"
                      :handlers [{:name :title
                                  :selector (create-default-text-selector
                                              [:header.article-header :h1.title])}
                                 {:name :article
                                  :selector (create-default-text-selector
                                              [:div.article-body-content :> #{:p :h3}])}
                                 {:name :extra
                                  :selector (fn [nodes]
                                              (-> nodes
                                                  (enlive/select [:span.media-element-container :div.share-buttons])
                                                  (->> (map #(-> % :attrs :data-media http-util/url-decode)))
                                                  (->> (str/join "\n"))))}]
                      :cond "html like '%article-header%' and html like '%article-body-content%'"}))


(defn parse-pegfitzpatrick
  "https://pegfitzpatrick.com"
  []
  (do-parse-and-save {:domain "https://pegfitzpatrick.com"
                      :handlers [{:name :title
                                  :selector (create-default-text-selector
                                              [:main.content :header.entry-header :h1.entry-title])}
                                 {:name :article
                                  :selector (create-default-text-selector
                                              [:main.content :div.entry-content :> #{:p :h2}])}
                                 {:name :extra
                                  :selector (fn [nodes]
                                              (-> nodes
                                                  (enlive/select [:div.sw-pinit :img])
                                                  (->> (map #(-> % :attrs :src)))
                                                  (->> (str/join "\n"))))}]
                      :cond "1=1"}))



(defn get-one-test []
  (let [selector (create-default-image-selector
                   [:div.a-body__content :figure :div.bb-figure__wrapper :> :img])]
    (selector (-> (get-by-id article-db {:table "source_article" :id 16762})
                  :html
                  (StringReader.)
                  (enlive/html-resource)))))
