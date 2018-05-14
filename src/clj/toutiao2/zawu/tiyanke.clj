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

(mount/start #'shop-datasource)

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
            (update :dealId str)
            (assoc :original_data (str (json/generate-string data))))))

(defn parse-item-data [data]
  (some-> data
          (parse-map [[:name [:title]]
                      [:lat [:latitude]]
                      [:lng [:longitude]]
                      :areaname
                      :backCateName
                      :city
                      :address
                      [:shop_id [:id]]])
          (assoc :original_data (str (json/generate-string data)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; 业务逻辑
(defn get-all-list [kword city cateid areaid]
  (loop [page 1]
    (let [source (get-list-data kword city cateid areaid page)
          list (get-in source [:data :searchResult])]
      (when (and (not-empty list)
                 (< page 40))
        (doseq [item list]
          (save-simple-data! :tiyanke_shop
                             (parse-item-data item)
                             :shop_id)
          (doseq [deal (:deals item)]
            (Thread/sleep 1000)
            (println "==================")
            (some-> (get deal :id)
                    (parse-detail-data)
                    (#(utils/with-try
                        (save-simple-data! :tiyanke_detail % :dealId))))))
        (recur (+ page 1))))))


#_(get-all-list "体验课" 10 -1 6)

(def areas [28 29 30 32 33 9553 31 9535 23420])
#_(doseq [area areas]
  (get-all-list "体验课" 30 -1 area))

#_(get-detail-data 34628388)

#_(println (:deselect-all (parse-detail-data "40330420")))
#_(let [data (parse-detail-data "40330420")]
  (save-simple-data! :tiyanke_detail data :dealId))


#_(get-list-data "体验课" 10 -1 6 35)
"\"imageText\":(.*?),\"navbar\""
