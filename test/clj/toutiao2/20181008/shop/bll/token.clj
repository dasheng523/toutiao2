(ns toutiao2.shop.bll.token
  (:require [toutiao2.cache :as cache]
            [toutiao2.shop.cache :as scache]))


(defn- get-token-data [k token]
  (cache/get-cache-live! scache/token-cache (str k ":" token)))

(defn set-token-data! [k v token]
  (cache/set-cache! scache/token-cache (str k ":" token) v))

(defmacro defget
  [& ks]
  (let [com (for [k ks]
              (let [fname (symbol (str "get-" (name k)))]
                `(def ~fname (partial get-token-data ~k))))]
    `(do ~@com)))

(defmacro defset
  [& ks]
  (let [com (for [k ks]
              (let [fname (symbol (str "set-" (name k)))]
                `(def ~fname (partial set-token-data! ~k))))]
    `(do ~@com)))

(defget :lang :user-id)
(defset :lang :user-id)


(defn default-token-data []
  {:lang :cn})

(defn init-token! [token]
  (let [default (default-token-data)]
    (doseq [k (keys default)]
      (cache/set-cache! scache/token-cache
                        (str k token)
                        (get default k)))))
