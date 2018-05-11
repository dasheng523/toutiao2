(ns toutiao2.arikami.emailtest
  (:require  [clojure.test :as t]
             [postal.core :as email]))

(email/send-message {:host "smtp.exmail.qq.com"
                     :user "business-us@actopp.com"
                     :pass "TUzhifu123"}
                    {:from "business-us@actopp.com"
                     :to "dasheng523@163.com"
                     :subject "Hi"
                     :body [{:type "text/html"
                             :content "<b>Test!</b>"}]})
