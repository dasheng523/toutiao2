(ns toutiao2.learn.mydb
  (:require [toutiao2.db.core :as db]
            [cheshire.core :as json]))



(db/get-first :test :id 1)

(db/save-simple-data! :test {:id 2 :content (json/parse-string (json/generate-string {:a 1 :b 2}) true) :name "5566"})


(json/parse-string (json/generate-string {:a 1 :b 2}))


