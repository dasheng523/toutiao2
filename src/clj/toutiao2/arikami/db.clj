(ns toutiao2.arikami.db
  (:require [hugsql.core :as hugsql]
            [clojure.string :as str]))


(def arikami-test-db
  {:classname "com.mysql.jdbc.Driver"
   :subprotocol "mysql"
   :subname "//45.77.203.61/test_arikami"
   :user "test"
   :password "1qa@WS3ed"
   :sslmode "require"})

(def arikami-db
  {:classname "com.mysql.jdbc.Driver"
   :subprotocol "mysql"
   :subname "//45.77.203.61/arikami"
   :user "arikami"
   :password "CgzqxZ@S3hYUi4yri"
   :sslmode "require"})

(hugsql/def-db-fns "sql/arikami.sql" {:quoting :mysql})
(hugsql/def-sqlvec-fns "sql/arikami.sql" {:quoting :mysql})

(defn get-options
  ([attribute_id arikami-db]
   (get-attribute-option-by-attrid arikami-db {:attrid attribute_id}))
  ([attribute_id]
   (get-options attribute_id arikami-db)))

(defn get-all-store
  ([arikami-db]
   (get-all-store-data arikami-db))
  ([]
   (get-all-store arikami-db)))

(defn delete-attrubute-option
  ([attrid arikami-db]
   (let [options (map #(:option_id %) (get-options attrid))]
     (when-not (empty? options)
       (delete-attribute-option-value-by-options arikami-db {:options options})
       (delete-attribute-option-swatch-by-options arikami-db {:options options})
       (delete-attribute-option-by-attrs arikami-db {:attrid attrid}))))
  ([attrid]
   (delete-attrubute-option attrid arikami-db)))


(defn insert-options
  ([attribute_id option-data arikami-db]
   (let [options (remove nil? option-data)
         last-option-id (:option_id (get-last-attribute-option arikami-db))]
     (insert-attribute-option-tuple arikami-db {:datas (map (fn [_] [attribute_id 0]) options)})
     (let [options-ids (map :option_id
                            (get-attribute-options-by-attrid-and-id
                             arikami-db
                             {:attrid attribute_id :id last-option-id}))
           optionValData (map #(identity [%1 0 %2]) options-ids options)
           store-ids (map :store_id (get-all-store))
           optionSwatchData (for [store-id store-ids
                                  option-val-info optionValData]
                              [(first option-val-info) store-id 0 (last option-val-info)])]
       (insert-attribute-option-value-tuple arikami-db {:datas optionValData})
       (insert-attribute-option-swatch-tuple arikami-db {:datas optionSwatchData}))))
  ([attribute_id option-data]
   (insert-options attribute_id option-data arikami-db)))


(defn get-attribute-option-value
  ([attrid arikami-db]
   (get-attribute-option-value-by-attrid arikami-db {:attrid attrid}))
  ([attrid]
   (get-attribute-option-value attrid arikami-db)))


(defn insert-option
  ([attribute_id text arikami-db]
   (let [rs (insert-attribute-option arikami-db
                                     {:attribute_id attribute_id :sort_order 0})
         store-ids (map :store_id (get-all-store))
         optionid (:generated_key rs)]
     (insert-attribute-option-value arikami-db {:option_id optionid
                                                :store_id 0
                                                :value text})
     (insert-attribute-option-swatch-tuple
      arikami-db
      {:datas (map #(identity [optionid % 0 text]) store-ids)})))
  ([attribute_id text]
   (insert-option attribute_id text arikami-db)))


(defn delete-all-category-index
  ([arikami-db]
   (delete-all-category-index-data arikami-db))
  ([]
   (delete-all-category-index arikami-db)))



