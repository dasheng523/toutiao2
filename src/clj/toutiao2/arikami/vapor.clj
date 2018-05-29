(ns toutiao2.arikami.vapor
  (:require [postal.core :as email]
            [toutiao2.utils :as utils]))

(def smtp {:host "smtp.exmail.qq.com"
           :user "business-us@actopp.com"
           :pass "TUzhifu123"})

(defn send-email [from to subject content]
  (email/send-message smtp
                      {:from from
                       :to to
                       :subject subject
                       :body [{:type "text/html"
                               :content content}]}))

(def goodslist (utils/csv-file->maps "e:/data/catalog_product_20180526_024555.csv"))

(def targetgoods
  (map (fn [n]
         (-> (update n :price #(* 1.2 (utils/parse-int %)))
             (dissoc :msrp_display_actual_price_type)))
       goodslist))


(utils/maps->csv-file targetgoods "e:/data/catalog_target.csv")

(utils/maps->csv-file (take 10 targetgoods) "e:/data/ddd.csv")

(utils/maps->csv-file (take 10 goodslist) "e:/data/source.csv")

#_(def content (slurp "/Users/huangyesheng/Desktop/vapor.html"))

#_(send-email "business-us@actopp.com"
            "398822391@qq.com"
            "THE BIG CONFIDENCE PLAN, VAPOR BY ARIKAMI PRESENTS YOU"
            content)
