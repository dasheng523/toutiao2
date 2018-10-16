(ns toutiao2.utils
  (:require [clojure.spec.alpha :as s]
            [clj-http.client :as http]
            [dk.ative.docjure.spreadsheet :as sheet]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [toutiao2.config :refer [env]]
            [net.cgrand.enlive-html :as enlive]
            [slingshot.slingshot :refer [throw+ try+]]
            [clj-uuid :as uuid]
            [digest :as digest])
  (:import (java.io StringReader PushbackReader FileReader FileWriter File)))


(s/def :http-response/status (s/and int? #(= 200 %)))
(s/def :http-response/body string?)
(s/def ::http-response (s/keys :req-un [:http-response/status :http-response/body]))

(defn get-ex
  [url]
  (-> url
      (http/get)
      (->> (s/assert ::http-response))
      :body))

(defn post-ex
  ([url]
   (post-ex url nil))
  ([url params]
    (-> url
        (http/post {:form-params params})
        (->> (s/assert ::http-response))
        :body)))

(defn- change-multipart-format
  [params]
  (for [k (keys params)]
    {:name (name k) :content (get params k)}))


(defn post-multipart-ex
  ([url]
   (post-ex url nil))
  ([url params]
   (-> url
       (http/post {:multipart (change-multipart-format params)})
       (->> (s/assert ::http-response))
       :body)))


(defn- parse-excel-data [list]
  (cons (map name (keys (first list))) (map #(vals %) list)))

(defn save-to-excel
  ([data]
   (save-to-excel data "e:/exportExcel.xlsx"))
  ([data file]
   (let [wb (sheet/create-workbook "html" (parse-excel-data data))]
     (sheet/save-workbook! file wb))))

(defn save-raw-data-excel
  [data file]
  (let [wb (sheet/create-workbook "html" data)]
    (sheet/save-workbook! file wb)))

(defn read-csv [file]
  (with-open [reader (io/reader file)]
    (doall
     (csv/read-csv reader))))


(defn list-data->maps [csv-data]
  (map zipmap
       (->> (first csv-data) ;; First row is the header
            (map keyword) ;; Drop if you want string keys instead
            repeat)
       (rest csv-data)))

(defn csv-file->maps [file]
  (-> (read-csv file)
      (list-data->maps)))

(defn trunc
  [s n]
  (subs s 0 (min (count s) n)))

(defn parse-int [s]
  (if-not (empty? s)
    (Integer. (re-find  #"\d+" s ))
    0))

(defn maps->csv-file [data file]
  (with-open [writer (io/writer file)]
    (csv/write-csv
      writer
      (cons (map #(name %) (keys (first data)))
            (map #(vals %) data)))))

(defn data->csv-file [head body file]
  (with-open [writer (io/writer file)]
    (csv/write-csv
     writer
     (cons head body))))

(defn read-excel [file tab cols]
  (->> (sheet/load-workbook file)
       (sheet/select-sheet tab)
       (sheet/select-columns cols)))

(defn read-excel->map [file tab]
  (->> (sheet/load-workbook file)
       (sheet/select-sheet tab)
       (sheet/row-seq)
       (remove nil?)
       (map sheet/cell-seq)
       (map #(map sheet/read-cell %))
       (list-data->maps)))


(defn find-first-in-list [pred list]
  (first (drop-while (comp not pred) list)))


(defn replace-in-list [coll n x]
  (concat (take n coll) (list x) (nthnext coll (inc n))))

(defn copy-file [source-path dest-path]
  (io/copy (io/file source-path) (io/file dest-path)))

(defn current-time []
  (quot (System/currentTimeMillis) 1000))

(defn md5 [s]
  (digest/md5 s))

(defn sha-256 [file]
  (digest/sha-256 file))

(defn rand-string
  "生成随机字符串，但不是唯一的"
  ([] (rand-string 8))
  ([n]
   (let [chars (map char (range 33 127))
         password (take n (repeatedly #(rand-nth chars)))]
     (reduce str password))))

(defn rand-idstr
  "生成随机字符串，唯一的"
  []
  (str (uuid/v1)))

(defn list-functions [namespace]
  (keys (ns-publics namespace)))



(defn serialize
  "Save a clojure form to a file"
  [file form]
  (with-open [w (FileWriter. (clojure.java.io/file file))]
    (print-dup form w)))

(defn deserialize
  "Load a clojure form from file."
  [file]
  (with-open [r (PushbackReader. (FileReader. (clojure.java.io/file file)))]
    (read r)))

(defmacro with-try [& body]
  `(try
     ~@body
     (catch Exception e#
       (log/error e#))))

(defn compose-url
  "拼成完成URL"
  [current-url target-url]
  (let [parts (str/split current-url #"/")
        domain (str (first parts) "//" (nth parts 2))
        path (str/join "/" (drop-last (str/split current-url #"/")))]
    (cond
      (str/starts-with? target-url "http") target-url
      (str/starts-with? target-url "/") (str domain target-url)
      :else (str path "/" target-url))))

(defn lazy-contains? [col k]
  (some #{k} col))

(defn get-upload-path []
  (-> env :upload-path))

(defn take-while+
  [pred coll]
  (when-let [f (first coll)]
    (if (pred f)
      (cons f (lazy-seq (take-while+ pred (rest coll))))
      [f])))

(defn round2
  "Round a double to the given precision (number of significant digits)"
  [d precision]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))

(defn gcd
  "求最大公约数"
  [a b]
  (if (zero? b)
    a
    (recur b (mod a b))))


(defn retry-on-error
  [dofn limit-times errmsg]
  (let [wrapfn #(try (dofn)
                     (catch Exception e :error-exception))
        queue (->> (repeatedly wrapfn)
                   (take-while+ #(= % :error-exception))
                   (take limit-times))
        rs (last queue)]
    (if (= rs :error-exception)
      (throw+ {:type :retry-error :msg errmsg})
      rs)))

(defn retry
  "dofn是执行函数。
  done?是判断是否完成的函数，入参是dofn的返回值，返回是true表示不需要重试，反之亦然
  try-time 重试次数
  fail-callback 重试达到上限，会调用此函数，入参是异常对象"
  [dofn done? try-time fail-callback]
  (let [trydofn #(try (dofn)
                      (catch Exception e {:error88 e}))
        ffn (fn [n]
              (let [rs (trydofn)]
                (if (and (nil? (:error88 rs)) (done? rs))
                  rs
                  (do (Thread/sleep 1000)
                      (if (>= n try-time)
                        (fail-callback (:error88 rs))
                        (recur (+ n 1)))))))]
    (ffn 1)))


