(ns toutiao2.shop.common
  (:require
   [toutiao2.utils :as utils]
   [clojure.set :as cset]))

(defmacro defnmap
  [fname & forms]
  (let [fns (reduce (fn [m [p & f]]
                      (let [pk (map keyword p)]
                        (assoc m (set pk) [(vec pk) `(fn ~p ~@f)])))
                    {}
                    forms)]
    `(def ~fname (fn [m#]
                   (if-let [ks# (->> m# keys set)]
                     (apply (second (get ~fns ks#))
                            (map (fn [v#] (get m# v#))
                                 (->> (get ~fns ks#) first))))))))



(defn ->success
  ([data msg]
   {:code 200
    :message msg
    :data data})
  ([data]
   (->success data "success")))

(defn ->fail
  ([data msg code]
   (merge {:code code
           :message msg
           :data data}))
  ([data msg]
   (->fail [data msg 800]))
  ([msg]
   (->fail [] msg 800)))

