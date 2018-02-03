(ns toutiao2.ave40.tools
  (:require [toutiao2.utils :as utils]
            [clojure.string :as str]
            [clojure.walk :as walk]))

(def product-filed [:color :atomizer_capacity :tier_prices
                    :product_id :special_price :name
                    :sku :weight :group_price :itemnum
                    :price :group_price_price :version])

(def empty-product-info (reduce #(assoc %1 %2 "") {} product-filed))

(def all-products
  (map (fn [info] (merge empty-product-info info))
         (utils/read-excel->map "E:/ave40_data/all_products_0201.xlsx" "all_products_0201")))
(def erp-products (map walk/stringify-keys (utils/read-excel->map "E:/ave40_data/20180203_erp产品表.xlsx" "库存查询")))
(def products-prices (utils/csv-file->maps "E:/ave40_data/20180203_pruduct-prices-2.csv"))

(def erp-merge-fields ["产品标签" "核算成本CNY" "核算成本USD" "报关成本CNY" "报关成本USD"])
(def price-merge-fileds [:sales_price])

(defn map-data [source-data merge-data merge-keys mapfn]
  (map (fn [info]
         (if-let [merge-item (first (filter (partial mapfn info) merge-data))]
           (reduce #(assoc %1 %2 (get merge-item %2 "")) info merge-keys)
           (reduce #(assoc %1 %2 "") info merge-keys)))
       source-data))

(def aaa-data
  (->
    all-products
    (map-data
      products-prices
      price-merge-fileds
      #(= (str (utils/parse-int (str (get %1 :product_id))))
          (str (get %2 :product_id))))
    (map-data erp-products
              erp-merge-fields
              #(= (str (get %1 :itemnum))
                  (str (get %2 "产品编码"))))))


(utils/save-to-excel aaa-data "e:/ave40_data/rs.xlsx")

