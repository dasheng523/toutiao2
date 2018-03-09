(ns toutiao2.arikami.account)

(defn- authenticate-common
  "普通的用户名密码类型的验证"
  [username password]
  )




(defmulti login [login-data])
