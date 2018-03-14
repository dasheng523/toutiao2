(ns toutiao2.shop.account
  (:require
   [toutiao2.shop.db :refer :all]
   [toutiao2.utils :as utils]
   [honeysql.helpers :refer :all :as helpers]
   [honeysql.core :as sql]
   [clojure.java.jdbc :as jdbc]
   [clojure.tools.logging :as log]))

(defprotocol Account
  (save [account])
  (reset-password [accountt password]))

(defprotocol AuthRequst
  (authenticate [req]))

(defn- encrypt
  "加密"
  [s seed]
  (utils/md5 (str s seed)))

(defn- get-common-account-by-name
  "通过用户名获取普通账号"
  [username]
  (-> (select :*)
      (from :common_account)
      (where [:= :username username])
      (sql-query)
      (first)))

(defn- authenticate-common-account
  "普通的用户名密码类型的验证"
  [username password]
  (if-let [account (get-common-account-by-name username)]
    (-> (select :id)
        (from :common_account)
        (where [:= :username username]
               [:= :password (encrypt password (:seed account))]
               [:= :is_active 1])
        (sql-query)
        (first))))


(defn- save-common-account!
  "保存授权账号"
  [account]
  (save-simple-data! :common_account account :username))


(defn create-common-account
  "创建普通的授权账号"
  [{:keys [user_id username password is_active]
    :or {is_active true}}]
  (let [seed (utils/rand-string)
        account {:username username
                 :password (encrypt password seed)
                 :user_id user_id
                 :seed seed
                 :is_active is_active
                 :id (utils/rand-idstr)}]
    ^{:type :common} account))


(defn- change-password-common
  "更改密码"
  [account password]
  (assoc account
         :password
         (encrypt password (:seed account))))


(defmacro defabc1
  [name & [p & f]]
  `(def ~name (fn ~@p ~@f)))

(defmacro defabc2
  [fname & forms]
  (let [fns (reduce (fn [m [p & f]]
                      (let [pk (map keyword p)]
                        (assoc m (set pk) [(vec pk) `(fn ~p ~@f)]))
                      )
                    {}
                    forms)]
    `(def ~fname (fn [m#]
                   (if-let [ks# (->> m# keys set)]
                     (apply (second (get ~fns ks#))
                            (map (fn [v#] (get m# v#))
                                 (->> (get ~fns ks#) first))))
                   #_(let [ks (->> m# keys (map #(-> % name symbol)))]
                     ks)
                 #_(let [ks (map #(-> % name symbol) (keys m))
                         vfn (get ~fns (set ks))]
                     (second vfn))))))


(defabc2 ttt
  ([a b] (+ a b))
  ([a c] (- a c)))

(ttt {:a 1 :c 3})

(macroexpand-1 '(defabc2 ttt
                  ([a b] (+ a b))
                  ([a c] (- a c))))



(defmacro sss [name]
  `(def ~name (fn [m#] (+ 1 2))))


(sss cc)
(cc 1)

(fn [a b] {'#{a b} 1})

(ttt 1 2)


(=
 (map #(-> % name symbol) [:a :b])
 '(a b))


(symbol (name :a))

(defn aaa []
  (let [fn1 (fn [a b] (+ a b))
        fn2 (fn [a b] (- a b))
        a1 '[a b]
        a2 '[a c]
        mm {'[a b] (fn [a b] (+ a b)),
            '[a c] (fn [a c] (- a c))}
        p1 (->> a1 (map name) (set))
        p2 (->> a2 (map name) (set))]
    (fn [m]
      (let [ks (->> m keys set)]
        (condp = ks
          p1 (apply fn1 (map #(get m %) (map keyword a1)))
          p2 (apply fn2 (map #(get m %) (map keyword a2)))
          (str "error"))))))

((aaa) {:b 1 :a 2})


(apply #(+ %1 %2) (map #(get {:a 1 :b 2} (keyword %)) `[a b]))

(map keyword '[a b])
{:a 1 :b 2}
(->> `[a b] (map keyword))



(= (map name [:a :b]) (map name `[a b]))


(abc {:a 1 :b 2})  ;; = 3
(abc {:b 2 :d 2})  ;; = 0

(defmulti authenticate
  (fn [auth-request] (vec (keys auth-request))))
(defmethod authenticate [:username :password]
  [{:keys [username password]}]
  (authenticate-common-account username password))

#_(authenticate {:username "test" :password "7788"})


#_(->
 (create-common-account {:username "test" :password "test55663" :user_id "1"})
 (change-password-common "7788")
 (save-common-account!))

#_(authenticate-common-account "test" "7788")


