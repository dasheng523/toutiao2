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
