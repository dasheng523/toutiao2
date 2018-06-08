(ns toutiao2.zawu.tiyanke
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [mount.core :refer [defstate] :as mount]
            [hikari-cp.core :as hikari]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers :exclude [update]]
            [clojure.tools.logging :as log]
            [toutiao2.utils :as utils]
            [clojure.string :as str]))


;;;;;;;;;;;;;;;;;;;;;;;; 数据库
(def db-connection {:adapter            "mysql"
                    :username           "root"
                    :password           "a5235013"
                    :database-name      "shop"
                    :server-name        "127.0.0.1"
                    :port-number        3306})

(defstate shop-datasource
  :start (do
           (log/info "starting db...")
           {:datasource (hikari/make-datasource db-connection)})
  :stop (do
          (log/info "stoping db...")
          (hikari/close-datasource (:datasource shop-datasource))))

#_(mount/start #'shop-datasource)

(defn sql-query
  "查询sql-map，并返回结果"
  [sql-map]
  (-> sql-map
      (sql/format)
      (->> (jdbc/query shop-datasource))))

(defn sql-execute!
  "执行sql"
  [sql-map]
  (-> sql-map
      (sql/format)
      (->> (jdbc/execute! shop-datasource))
      (first)))

(defn get-first
  "简单查询第一个并返回"
  [table k v]
  (-> (select :*)
      (from table)
      (where [:= k v])
      (sql-query)
      (first)))

(defn save-simple-data!
  "保存简单数据"
  ([table data idkey]
   (let [idval (get data idkey)
         insert-fn #(-> (insert-into table)
                        (values [data]))
         update-fn #(-> (helpers/update table)
                        (sset data)
                        (where [:= idkey idval]))
         sqlmap (if (get-first table idkey idval)
                  (update-fn)
                  (insert-fn))]
     (sql-execute! sqlmap)))
  ([table data]
   (save-simple-data! table data :id)))

#_(save-simple-data! :tiyanke_shop {:shop_id "111"})

;;;;;;;;;;;;;;;;;;;;;;;; 抓取

(def user-agent "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.162 Safari/537.36")

(defn generate-cookies []
  (str "uuid=2af9d5bb1152b" (str (+ 9000000 (rand-int 999999))) ".1525442542.0.0.0; oc=JEjjQ7Jc8c-lNlb2siBYJpEHpJlxC39C-I5qe8HP8QFzrTdlB_H01fixKV3cei8pXq_C9y4ksVKlPhRauSaN2urkXPWRaTycWW6otgtzztLqJwJ00tflfMOZ8p7WI0j1ji8cBwi8vY5jLqctRfNlbh-JUQLRxGVhMw4AYFYqWw8; _lxsdk_cuid=1632b763347c8-026fa93fe70685-336c7b05-fa000-1632b763347c8; __utma=211559370.1361816146.1525442560.1525442560.1525442560.1; __utmz=211559370.1525442560.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); __utmv=211559370.|1=city=shenzhen=1; ci=30; rvct=30%2C301; __mta=152508678.1525442948211.1526177131313.1526180985582.10; _lx_utm=utm_source%3DBaidu%26utm_medium%3Dorganic; client-id=b0e1aaa0-6ff2-48be-a0f1-b5868d679dad; _lxsdk_s=1635ed082d1-3de-d-aea%7C%7C7"))

(defn get-list-data [kword city cateId areaId page]
  (let [pagesize 32
        offset (* page pagesize)
        url (str "http://apimobile.meituan.com/group/v4/poi/pcsearch/" city "?uuid=4d768a970e22495f992d.1525314072.1.0.0&userid=-1&limit=" pagesize "&offset=" offset "&cateId=" cateId "&q=" kword "&areaId=" areaId)]
    (some-> url
            (http/get {:headers {"User-Agent" user-agent
                                 "Cookie" (generate-cookies)}})
            :body
            (json/parse-string true))))


(defn get-detail-data [id]
  (let [url (str "http://www.meituan.com/deal/" id ".html")
        result-html (-> (http/get url {:headers {"User-Agent" user-agent
                                                 "Cookie" (generate-cookies)}})
                        :body)
        data (some-> (re-find #"_appState = (.*?);</script>" result-html)
                     second
                     (json/parse-string true)
                     (dissoc :comHeader)
                     (dissoc :comFooter))]
    data))


(defn parse-map [data rules]
  (->> (map (fn [item]
             (if (keyword? item)
               [item (get data item)]
               [(first item) (get-in data (second item))]))
           rules)
      (into {})))

(defn parse-detail-data [detail-id]
  (let [data (get-detail-data detail-id)]
    (some-> data
            (parse-map [[:dealName [:dealInfo :dealName]]
                        [:description [:dealInfo :terms]]
                        :dealId
                        [:price [:dealInfo :price]]
                        [:price_original [:dealInfo :value]]
                        [:discount [:dealInfo :discount]]
                        [:usefulTime [:dealInfo :usefulTime]]
                        [:images [:dealInfo :imageText]]
                        [:shop_id [:poiList :dealPoisInfo 0 :poiId]]
                        [:phone [:poiList :dealPoisInfo 0 :phone]]])
            (update :images #(str/join ";" (map :image %)))
            (update :dealId str))))

(defn parse-item-data [data]
  (some-> data
          (parse-map [[:name [:title]]
                      [:lat [:latitude]]
                      [:lng [:longitude]]
                      :areaname
                      :backCateName
                      :city
                      :address
                      [:shop_id [:id]]])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; 业务逻辑
(defn get-all-list [kword city cateid areaid]
  (loop [page 1]
    (let [source (get-list-data kword city cateid areaid page)
          list (get-in source [:data :searchResult])]
      (when (and (not-empty list)
                 (< page 40))
        (doseq [item list]
          (println "==================")
          (save-simple-data! :tiyanke_shop
                             (parse-item-data item)
                             :shop_id)
          (utils/with-try (save-simple-data! :tiyanke_origin
                                             {:type 1
                                              :data (json/generate-string item)
                                              :entity_id (str "1-" (get item :id))}
                                             :entity_id))
          (doseq [deal (:deals item)]
            (Thread/sleep 500)
            (println (get deal :id))
            (when-let [dealdata (some-> (get deal :id)
                                        (parse-detail-data))]
              (some-> dealdata
                      (#(utils/with-try
                          (save-simple-data! :tiyanke_detail % :dealId)
                          (save-simple-data! :tiyanke_origin
                                             {:type 2
                                              :data (json/generate-string dealdata)
                                              :entity_id (str "2-" (get deal :id))}
                                             :entity_id)))))))
        (recur (+ page 1))))))


(defn get-city-areas [url]
  (some-> url
          (http/get {:headers {"User-Agent" user-agent
                               "Cookie" (generate-cookies)}})
          :body
          (->> (re-find #"children\":(.*?),\"checked"))
          (second)
          (json/parse-string true)
          (->> (mapcat (fn [item]
                         (map :id (get item :children))))
               (into #{}))))

(def citys [{:id 10 :url "http://sh.meituan.com/" :name "上海"}
            {:id 20 :url "http://gz.meituan.com/" :name "广州"}
            {:id 30 :url "http://sz.meituan.com/" :name "深圳"}
            {:id 40 :url "http://tj.meituan.com/" :name "天津"}
            {:id 42 :url "http://xa.meituan.com/" :name "西安"}
            {:id 45 :url "http://cq.meituan.com/" :name "重庆"}
            {:id 50 :url "http://hz.meituan.com/" :name "杭州"}
            {:id 55 :url "http://nj.meituan.com/" :name "南京"}
            {:id 55 :url "http://nj.meituan.com/" :name "南京"}
            {:id 59 :url "http://cd.meituan.com/" :name "成都"}
            {:id 57 :url "http://wh.meituan.com/" :name "武汉"}])

(defn fecth-city-tiyanke [city]
  (let [id (get city :id)
        areas (some-> (get city :url)
                      (str "s/体验课/")
                      (get-city-areas))]
    (doseq [area areas]
      (println area id)
      (get-all-list "体验课" id -1 area))))

#_(defonce futs (map #(future (fecth-city-tiyanke %)) citys))

#_(future-done? (second futs))




#_(doseq [area beijing-areas]
  (get-all-list "体验课" 1 -1 area))

#_(println (:deselect-all (parse-detail-data "40330420")))
#_(let [data (parse-detail-data "40330420")]
  (save-simple-data! :tiyanke_detail data :dealId))

#_(get-list-data "体验课" 10 -1 6 35)
