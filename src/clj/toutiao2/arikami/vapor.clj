(ns toutiao2.arikami.vapor
  (:require [postal.core :as email]
            [toutiao2.utils :as utils]
            [clojure.tools.logging :as log]
            [toutiao2.config :as config]))

(def smtp {:host "smtp.exmail.qq.com"
           :user "ecig-wholesale@arikami.com"
           :pass "Bb12BB!b,a"})

(def smtp-gmail {:host "smtp.gmail.com"
                 :user "vapor.arikami@gmail.com"
                 :pass "arikami38460"})

(defn send-email [smtp from to subject content]
  (email/send-message smtp
                      {:from from
                       :to to
                       :subject subject
                       :body [{:type "text/html"
                               :content content}]}))

(defn- upload-path []
  (cond (config/isMac?) (-> config/env :mac-dired-path)
        (config/isWindows?) (-> config/env :win-dired-path)
        :else (-> config/env :linux-dired-path)))


#_(def content (slurp (str (upload-path) "/vapor.html")))
(def content "")

#_(def customers (utils/read-excel->map
                (str (upload-path) "/customers-ref.xlsx")
                "html"))
(def customers "")

(defn send-customer-email [email]
  (send-email smtp-gmail
              "ecig-wholesale@arikami.com"
              email
              "THE BIG CONFIDENCE PLAN, VAPOR BY ARIKAMI PRESENTS YOU"
              content))

#_(println (send-customer-email "dasheng523@163.com"))
#_(println (send-customer-email "laurent.roger.segura@gmail.com"))

(defn do-logic []
  (loop [emails (map :ZEMAIL_0 (take 500 customers))]
    (let [email (first emails)]
      (log/info email)
      (try
        (send-customer-email email)
        (catch Exception e
          (log/error e)))
      (recur (rest emails)))))

#_(def goodslist (utils/csv-file->maps "e:/data/catalog_product_20180526_024555.csv"))

#_(def targetgoods
  (map (fn [n]
         (-> (update n :price #(* 1.2 (utils/parse-int %)))
             (dissoc :msrp_display_actual_price_type)))
       goodslist))


#_(utils/maps->csv-file targetgoods "e:/data/catalog_target.csv")

#_(utils/maps->csv-file (take 10 targetgoods) "e:/data/ddd.csv")

#_(utils/maps->csv-file (take 10 goodslist) "e:/data/source.csv")


#_(do-logic)
