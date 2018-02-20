(ns toutiao2.ave40.push
  (:require [clj-http.client :as http]
            [toutiao2.ave40.db :refer :all]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as enlive]
            [toutiao2.ave40.utils :as utils]
            [clojure.walk :as w]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]))

(defn- push-article
  "将文章推送到指定的博客"
  [domain {:keys [title article]}]
  (log/info (str "pushing to " domain ", article: " title))
  (println (:body
             (http/post "http://manage.ecigview.com/posts/create"
                        {:form-params {:domain domain
                                       :title title
                                       :content article}}))))

(defn- add-image-to-article [article images]
  "将图片加到文章中"
  (let [pies (str/split article #"\n")
        pies-count (count pies)
        images-count (count images)
        do-fn (fn [pie image] (str (if image (str "<img src=\"" image "\">")) "\n" pie))
        images-com (if (> pies-count images-count)
                     (concat images (repeat (- pies-count images-count) nil))
                     images)]
    (-> (map do-fn pies images-com)
        (->> (remove nil?))
        (->> (str/join "\n")))))


(defn- wrap-paragraph [text]
  "包裹段落"
  (apply str (map #(apply str (enlive/emit* ((enlive/wrap :p) %)))
                  (str/split text #"\n"))))

(defn patch-push-article
  "批量推送文章到博客"
  [domain amount tag]
  (let [limit-where (-> (select-all article-db {:table "source_site" :where (str "tag='" tag "'")})
                        (->> (map #(str "source_url like '" (:domain %) "%'")))
                        (->> (str/join " or "))
                        (#(str "(" % ")")))
        article-list (select-all article-db
                      {:table "articles"
                       :cols  ["id" "spinner_title" "spinner_article" "extra" "source_url"]
                       :where (str limit-where " and ISNULL(post_domain) and not isnull(spinner_title) and not isnull(spinner_article) and spinner_article<>'' limit " amount)})]
    (doseq [article-info article-list]
      (let [extra (-> article-info :extra)
            image-urls (if (and extra (not= "" extra))
                   (-> extra
                       (json/parse-string true)
                       :images
                       (->> (map #(-> (select-one article-db {:table "image" :where (str "id=" %)}) :url))))
                   (-> (select-rand-image article-db {:tag tag})
                       (->> (map :url))))]
        (push-article domain {:title (:spinner_title article-info)
                              :article (add-image-to-article (:spinner_article article-info) image-urls)})
        (update-data article-db {:table "articles"
                                 :updates {:post_time (quot (System/currentTimeMillis) 1000)
                                           :post_domain domain}
                                 :where (str "id=" (:id article-info))})))))

(def domains [{:domain "www.vapinggift.com" :tag "vape" :amount 2}
              {:domain "www.ecigview.com" :tag "vape" :amount 2}
              {:domain "www.eciggadget.com" :tag "vape" :amount 2}
              {:domain "www.ecigsmok.com" :tag "vape" :amount 2}
              {:domain "www.vapingblog.net" :tag "vape" :amount 2}
              {:domain "www.eciggod.com" :tag "vape" :amount 2}
              {:domain "www.vapingpromo.com" :tag "vape" :amount 2}
              {:domain "www.ecigcommunity.com" :tag "vape" :amount 2}
              {:domain "www.ecigblog.in" :tag "vape" :amount 2}
              {:domain "www.vapingblog.in" :tag "vape" :amount 2}
              {:domain "www.vaping10.com" :tag "vape" :amount 2}
              {:domain "www.betalily.com" :tag "fashion" :amount 2}
              {:domain "www.scan2car.com" :tag "car" :amount 2}
              {:domain "www.rebornbee.com" :tag "health" :amount 2}
              {:domain "www.markasblogger.com" :tag "blogger" :amount 2}])

(defn do-push []
  (doseq [{:keys [domain tag amount]} domains]
    (patch-push-article domain amount tag)))

#_(do-push)
#_(patch-push-article "www.markasblogger.com" 5 "blogger")
