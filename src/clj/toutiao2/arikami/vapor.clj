(ns toutiao2.arikami.vapor
  (:require [postal.core :as email]
            [toutiao2.utils :as utils]
            [clojure.tools.logging :as log]))

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

(def content (slurp "/Users/huangyesheng/Desktop/vapor.html"))

(def customers (utils/read-excel->map
                 "/Users/huangyesheng/Desktop/customers-ref.xlsx"
                 "html"))

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

#_(do-logic)
