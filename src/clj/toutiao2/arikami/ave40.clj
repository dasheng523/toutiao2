(ns toutiao2.arikami.ave40
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [toutiao2.utils :as utils]))

  (def api-customer-url "http://61.144.170.188:1496/OPEN/api/Customer/ERPCUSTOMER")
  (def api-products-url "http://61.144.170.188:1496/OPEN/api/Item/Query")


(defn post-data [url req]
  (-> (http/post url {:body (json/generate-string req)
                      :accept :json
                      :content-type :json})
      :body
      (json/parse-string true)))

(defn all-customers
  ([page]
   (let [req {"PageSize" 500
              "PageNumber" page
              "RequestEntity" {"REP_0" ""}
              "Token" "ave40@SAGE"}
         data (post-data api-customer-url req)
         result (get data :Result)
         pagenum (get data :PageNumber)
         totalpage (get data :TotalPages)]
     (println page)
     (lazy-seq (concat result
                       (if (> totalpage pagenum)
                         (all-customers (+ page 1)))))))
  ([]
   (all-customers 1)))

(defn all-products
  ([]
   (all-products 1))
  ([page]
   (let [req {"PageSize" 500
              "PageNumber" page
              "RequestEntity" {"ITMREF_0" ""}
              "Token" "ave40@SAGE"}
         data (post-data api-products-url req)
         result (get data :Result)
         pagenum (get data :PageNumber)
         totalpage (get data :TotalPages)]
     (println page)
     (lazy-seq (concat result
                       (if (> totalpage pagenum)
                         (all-products (+ page 1))))))))



(utils/save-to-excel
   (take 58888 (all-customers))
   "/Users/huangyesheng/Desktop/customers.xlsx")


#_(utils/save-to-excel
 (take 20000 (all-products))
 "/Users/huangyesheng/Desktop/products.xlsx")
