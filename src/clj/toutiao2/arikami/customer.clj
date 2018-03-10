(ns toutiao2.arikami.customer)

(defn create-customer-info
  "创建用户"
  [{:keys [id firstname lastname phone email] :as customer-info}]
  (assert (not-empty id))
  customer-info)

(defn save-customer-info
  "保存用户"
  [{:keys [id firstname lastname phone email] :as customer-info}]
  (assert (not-empty id))
  )


(defn stream-withdraw [balance amount-stream]
  (cons balance
        (lazy-seq (stream-withdraw (- balance (first amount-stream))
                                   (rest amount-stream)))))


(def ones (cons 1 (lazy-seq ones)))

(take 5 ones)

(def ss (stream-withdraw 1000 ones))

(take 5 ss)


