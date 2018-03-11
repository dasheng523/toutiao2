(ns toutiao2.shop.db
  (:require
   [clojure.tools.logging :as log]
   [toutiao2.config :refer [env]]
   [hikari-cp.core :as hikari]
   [mount.core :refer [defstate] :as mount]
   [clojure.java.jdbc :as jdbc]
   [honeysql.core :as sql]
   [honeysql.helpers :refer :all :as helpers]))

(defstate shop-datasource
  :start (do
           (log/info "starting shop db...")
           {:datasource (hikari/make-datasource (-> env :database-spec))})
  :stop (do
          (log/info "stop shop db...")
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
     (log/info sqlmap)
     (sql-execute! sqlmap)))
  ([data table]
   (save-simple-data! table data :id)))

