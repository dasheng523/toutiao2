(ns toutiao2.arikami.db
  (:require [hugsql.core :as hugsql]
            [clojure.string :as str]))


(def arikami-db
  {:classname "com.mysql.jdbc.Driver"
   :subprotocol "mysql"
   :subname "//45.77.203.61/test_arikami"
   :user "test"
   :password "1qa@WS3ed"
   :sslmode "require"})

(hugsql/def-db-fns "sql/arikami.sql" {:quoting :mysql})
(hugsql/def-sqlvec-fns "sql/arikami.sql" {:quoting :mysql})

(get-attribute-option-by-attrid arikami-db {:attrid 134})

(defn delete-attrubute-option [attrid]
  (delete-attribute-option-by-attrs
    arikami-db
    {:attrid attrid})
  (delete-attribute-option-value-by-options
    arikami-db
    {:options (map #(:option_id %)
                   (get-attribute-option-by-attrid arikami-db {:attrid attrid}))}))

