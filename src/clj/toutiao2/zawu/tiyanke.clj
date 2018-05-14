(ns toutiao2.zawu.tiyanke
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [mount.core :refer [defstate] :as mount]
            [hikari-cp.core :as hikari]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers :exclude [update]]
            [clojure.tools.logging :as log]
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
     (log/info sqlmap)
     (sql-execute! sqlmap)))
  ([table data]
   (save-simple-data! table data :id)))

#_(save-simple-data! :tiyanke_shop {:shop_id "111"})

;;;;;;;;;;;;;;;;;;;;;;;; 抓取

(def cookies "uuid=4d768a970e22495f992d.1525314072.1.0.0; __mta=41884189.1525314095769.1525314095769.1525314095769.1; ci=30; rvct=30; _lxsdk_cuid=163383b86ddc8-0f4f2624a7c373-3961430f-1fa400-163383b86ddc8; _lxsdk_s=163383b86df-d72-47f-b42%7C%7C2")
(def user-agent "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.162 Safari/537.36")

(defn get-list-data [kword city cateId areaId page]
  (let [pagesize 32
        offset (* page pagesize)
        url (str "http://apimobile.meituan.com/group/v4/poi/pcsearch/" city "?uuid=4d768a970e22495f992d.1525314072.1.0.0&userid=-1&limit=" pagesize "&offset=" offset "&cateId=" cateId "&q=" kword "&areaId=" areaId)]
    (some-> url
            (http/get {:headers {"User-Agent" user-agent
                                 "Cookie" cookies}
                       :debug true})
            :body
            (json/parse-string true))))


(defn get-detail-data [id]
  (let [url (str "http://www.meituan.com/deal/" id ".html")
        result-html (-> (http/get url {:headers {"User-Agent" user-agent
                                                 "Cookie" cookies}})
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
            (assoc :original_data (json/generate-string data)))))

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
          (assoc :original_data (json/generate-string data))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; 业务逻辑
(defn get-all-list [kword city cateid areaid]
  (loop [page 1]
    (let [source (get-list-data kword city cateid areaid page)
          list (get-in source [:data :searchResult])]
      #_(println source)
      (when (not-empty list)
        (doseq [item list]
          (save-simple-data! :tiyanke_shop
                             (parse-item-data item)
                             :shop_id)
          )))))

#_(get-all-list "体验课" 10 -1 6)

#_(println (:deselect-all (parse-detail-data "40330420")))
#_(let [data (parse-detail-data "40330420")]
  (save-simple-data! :tiyanke_detail data :dealId))


(get-list-data "体验课" 10 -1 6 35)
"\"imageText\":(.*?),\"navbar\""
