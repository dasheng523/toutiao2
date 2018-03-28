(ns toutiao2.zimeiti.config)


(def platform "mac")
(def download-base-path "/Users/huangyesheng/Documents/pics")
(def url-data "/Users/huangyesheng/Documents/pics/test.txt")
(def cookies-base-path "/Users/huangyesheng/Documents/cookies/")

#_(def platform "win")
#_(def download-base-path "e:\\pics")
#_(def url-data "e:\\test.txt")
#_(def cookies-base-path "e:\\cookies\\")


(def xf
  (comp
   (filter odd?)
   (map inc)
   (take 5)))

(eduction xf (range 5))

(into [] xf (range 1000))

(sequence xf (range 1000))


(defn duplicates-odd-vals [xf]
  (fn
    ([] (xf))
    ([result] (xf result))
    ([result input]
     (cond
       (odd? input)
       (-> result
           (xf input)             ; first odd value
           (xf input))            ; second odd value
       :ELSE        result ))))  ; do nothing

(transduce duplicates-odd-vals conj (range 11))
