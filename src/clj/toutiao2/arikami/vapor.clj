(ns toutiao2.arikami.vapor
  (:require [postal.core :as email]
            [toutiao2.utils :as utils]
            [clojure.tools.logging :as log]
            [toutiao2.config :as config]))

#_(def smtp {:host "smtp.exmail.qq.com"
           :user "ecig-wholesale@arikami.com"
           :pass "Bb12BB!b,a"})

(def smtp-gmail {:host "smtp.gmail.com"
                 :user "vapor.arikami@gmail.com"
                 :pass "arikami38460"
                 :ssl true})

(def gmail-list
  ["vapor.arikami@gmail.com"])

(defn gmail-smtp-gmail [list]
  (map #(merge {:host "smtp.gmail.com"
                :user %
                :pass "arikami38460"
                :ssl true})
       list))

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


(defn send-customer-email [from-smtp email content]
  (send-email from-smtp
              "vapor.arikami@gmail.com"
              email
              "THE BIG CONFIDENCE PLAN, VAPOR BY ARIKAMI PRESENTS YOU"
              content))

(defn do-logic []
  (let [customers (utils/read-excel->map
                   (str (upload-path) "/customers-ref.xlsx")
                   "html")
        content (slurp (str (upload-path) "/vapor.html"))
        from-smtp (gmail-smtp-gmail gmail-list)]
    (loop [emails (map :ZEMAIL_0 customers)
           n 0]
      (let [email (first emails)]
        (log/info email)
        (try
          (send-customer-email (nth from-smtp
                                    (rem n (count from-smtp)))
                               email
                               content)
          (catch Exception e
            (log/error e)))
        (recur (rest emails) (+ n 1))))))

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
