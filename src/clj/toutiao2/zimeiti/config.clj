(ns toutiao2.zimeiti.config
  (:require [clojure.string :as str]
            [toutiao2.config :refer [env]]))

(def *cos*
  {:name (System/getProperty "os.name"),
   :version (System/getProperty "os.version"),
   :arch (System/getProperty "os.arch")})

(defn isWindows? []
  (str/includes? (str/lower-case (:name *cos*)) "window"))

(defn isMac? []
  (str/includes? (str/lower-case (:name *cos*)) "mac"))

(defn get-download-path []
  (if (isWindows?)
    (-> env :win-download-path)
    (-> env :mac-download-path)))

(defn get-cookies-path []
  (if (isWindows?)
    (-> env :win-cookies-path)
    (-> env :mac-cookies-path)))


(def xf
  (comp
   (filter odd?)
   (map inc)
   (take 5)))

#_(eduction xf (range 5))

#_(into [] xf (range 1000))

#_(sequence xf (range 1000))


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

#_(transduce duplicates-odd-vals conj (range 11))
