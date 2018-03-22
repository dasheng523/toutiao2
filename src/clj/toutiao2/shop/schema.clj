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

(defresp RespLogin
  resp-base
  {:username String})


(s/validate RespLogin {:code 200 :message "success" :data {:username "5566"}})
