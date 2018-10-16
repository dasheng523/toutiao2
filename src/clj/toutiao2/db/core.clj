(ns toutiao2.db.core
  (:require
    [clj-time.jdbc]
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :as log]
    [conman.core :as conman]
    [hikari-cp.core :as hikari]
    [honeysql.core :as sql]
    [honeysql.helpers :refer :all :as helpers :exclude [update]]
    [toutiao2.config :refer [env]]
    [mount.core :refer [defstate]])
  (:import [java.sql
            BatchUpdateException
            PreparedStatement]))


(defstate ^:dynamic *db-datasource*
  :start (if-let [jdbc-url (env :database-url)]
           (do
             (log/info "starting db ...")
             {:datasource (hikari/make-datasource {:jdbc-url (-> env :database-url)})})
           (do
             (log/warn "database connection URL was not found, please set :database-url in your config, e.g: prod-config.edn")
             *db-datasource*))
  :stop (do
          (log/info "closing db...")
          (hikari/close-datasource (:datasource *db-datasource*))))

(defn sql-query
  "查询sql-map，并返回结果"
  [sql-map]
  (-> sql-map
      (sql/format)
      (->> (jdbc/query *db-datasource*))))

(defn sql-execute!
  "执行sql"
  [sql-map]
  (-> sql-map
      (sql/format)
      (->> (jdbc/execute! *db-datasource*))
      (first)))


(defn get-first
  "简单查询第一个并返回"
  [table k v]
  (-> (select :*)
      (from table)
      (where [:= k v])
      (limit 1)
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
         sqlmap (if (and idval
                         (get-first table idkey idval))
                  (update-fn)
                  (insert-fn))]
     #_(log/info sqlmap)
     (sql-execute! sqlmap)))
  ([table data]
   (save-simple-data! table data :id)))
