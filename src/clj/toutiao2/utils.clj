(ns toutiao2.utils
  (:require [clojure.spec.alpha :as s]
            [clj-http.client :as http]
            [dk.ative.docjure.spreadsheet :as sheet]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [toutiao2.config :refer [env]]
            [net.cgrand.enlive-html :as enlive]
            [flake.core :as flake]
            [flake.utils :as flake-utils]
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
  (Integer. (re-find  #"\d+" s )))

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

(defn to-enlive [html]
  (-> html
      (StringReader.)
      (enlive/html-resource)))

(defn text-selector
  [nodes node-select]
  (str/join
    "\n"
    (map (fn [n]
           (enlive/text n))
         (enlive/select nodes node-select))))

(defn html-selector
  [nodes node-select]
  (when-let [find-rs (enlive/select nodes node-select)]
    (-> find-rs
        (enlive/emit*)
        (str/join))))

(defn href-selector
  "取第一个匹配结果的URL"
  [nodes node-select]
  (let [find-rs (enlive/select nodes node-select)]
    (if-not (empty? find-rs)
      (-> find-rs first :attrs :href))))

(defn hrefs-selector
  "取所有匹配结果的URL"
  [nodes node-select]
  (let [find-rs (enlive/select nodes node-select)]
    (if find-rs
      (map #(-> % :attrs :href) find-rs))))

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
  (->> (repeatedly flake/generate!)
       first
       flake/flake->bigint
       flake-utils/base62-encode))

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


(defn lazy-contains? [col k]
  (some #{k} col))

(defn get-upload-path []
  (-> env :upload-path))

