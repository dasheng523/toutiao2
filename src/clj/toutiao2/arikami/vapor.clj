(ns toutiao2.arikami.vapor
  (:require [postal.core :as email]))

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

#_(def content (slurp "/Users/huangyesheng/Desktop/vapor.html"))

#_(send-email "business-us@actopp.com"
            "398822391@qq.com"
            "THE BIG CONFIDENCE PLAN, VAPOR BY ARIKAMI PRESENTS YOU"
            content)
