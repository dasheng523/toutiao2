(ns toutiao2.arikami.shop)

(defn account-base
  "创建普通账号"
  [username password]
  {:username username
   :password password
   :grant-type :base})

;; 系统支持的社交平台
(def social-platform [:facebook :twiter :wechat])

(defn account-social
  "创建社媒账号"
  [openid platform]
  {:openid openid
   :platform platform
   :grant-type :social})



(defmulti authorie-account
  "认证账号信息并返回用户ID"
  #(:grant-type %))

(defmethod authorie-account :base
  [account]
  :todo)
(defmethod authorie-account :social
  [account]
  :todo)


(defn bind-account-info
  "绑定账户的其他信息"
  [account info]
  (assoc account :information info))

(defn save-account
  "保存账号信息"
  [account]
  :todo)


;; 注册
;; 登录
;; 忘记密码
(defn create-base-account [username password])
(defn create-social-account [openid platform])


(defn login [username password]
  :read-db
  :if-verify-set-login-state)

(defn login-platform [openid platform]
  :read-db
  :if-exist-set-login-state)

(defn register [account base-info]
  )

(defn create-empty-customer []
  {:name ""
   :accounts []})

(defn bind-customer-account
  "绑定客户账号"
  [customer account]
  (update customer :accounts conj account))

;; 如何持久化？
;; 硬要数据抽象
