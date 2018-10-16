(ns toutiao2.shop.schema
  (:require [schema.core :as s]))


(def resp-base
  {:code Long
   :message String})

(defmacro defresp
  "定义一个list"
  [respname base schema]
  `(s/defschema ~respname
     (merge ~base
            {:data ~schema})))

(defresp RespAuth
  resp-base
  {:first-name String
   :last-name String})
