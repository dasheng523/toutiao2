(ns toutiao2.routes.arikami
  (:require [toutiao2.layout :as layout]
            [toutiao2.config :refer [env]]
            [toutiao2.utils :as utils]
            [toutiao2.arikami.tools :as tools]
            [compojure.core :refer [defroutes GET POST]]
            [selmer.filters :refer [add-filter!]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:import [java.io File FileInputStream FileOutputStream]
           [java.nio.file Files LinkOption]
           [java.nio.file.attribute BasicFileAttributes]))

(defn get-upload-path []
  (-> env :upload-path))

(defn file-attributes [file]
  "Return the file BasicFileAttributes of file.  File can be a file or a string
   (or anything else acceptable to jio/file)"
  (Files/readAttributes (.toPath (io/file file))
                        BasicFileAttributes
                        (into-array LinkOption [])))

(defn upload-file-name []
  (map #(.getName %)
       (some-> (get-upload-path)
               (clojure.java.io/file)
               (file-seq)
               (->> (filter #(str/ends-with? (.getName %) ".xlsx"))
                    (sort-by #(.creationTime (file-attributes %))
                             #(compare %2 %1))))))


(defn import-page []
  (layout/render "arikami/import.html" {:filelist (upload-file-name)}))

(defn file-path [path & [filename]]
  (java.net.URLDecoder/decode
   (str path File/separator filename)
   "utf-8"))

(defn upload-file
  [path {:keys [tempfile size filename]}]
  (try
    (println (file-path path filename))
    (io/copy tempfile (io/file (file-path path filename)))))

(defn- getExcelData [filename]
  (-> (file-path (get-upload-path) filename)
      (utils/read-excel->map "upload_template")))


(defmacro with-try [& body]
  `(try
     ~@body
     (catch Exception e#
       (log/error e#)
       (response/ok {:error-msg "file content error"}))))

(defn verifySku [filename]
  (with-try
     (let [data (getExcelData filename)
           rs {:error-sku (->> data
                               (tools/verify-sku)
                               (remove nil?))}]
       (response/ok rs))))

(defn verifyImages [filename]
  (with-try
    (let [data (getExcelData filename)
          rs {:error-images (->> data
                                 (mapcat #(str/split (:image %) #";"))
                                 (set)
                                 (tools/verify-files)
                                 (remove nil?))}]
      (response/ok rs))))

(defn checkExcel [filename]
  (with-try
    (let [data (getExcelData filename)
          rs (tools/check-data data)]
      (response/ok {:rs (map str rs)}))))

(defn importAttr [filename]
  (with-try
    (let [data (getExcelData filename)]
      (tools/import-attribute-options (tools/attribute-dataset data))
      (response/ok {:state :success}))))

(defn importAttrTest [filename]
  (with-try
    (let [data (getExcelData filename)]
      (tools/import-attribute-options (tools/attribute-dataset data) "test")
      (response/ok {:state :success}))))

(defn delIndex [_]
  (with-try
    (tools/delIndex "prod")
    (response/ok {:state :success})))

(defn reIndex [_]
  (with-try
    (tools/reIndex "prod")
    (response/ok {:state :success})))

(defn flushCache [_]
  (with-try
    (tools/flush-cache "prod")
    (response/ok {:state :success})))

(defn delIndexTest [_]
  (with-try
    (tools/delIndex "test")
    (response/ok {:state :success})))

(defn reIndexTest [_]
  (with-try
    (tools/reIndex "test")
    (response/ok {:state :success})))

(defn flushCacheTest [_]
  (with-try
    (tools/flush-cache "test")
    (response/ok {:state :success})))

(defn csvHandler [convert-format target-name]
  (fn [filename]
    (with-try
      (let [data (getExcelData filename)]
        (tools/do-all-data-logic data (str (get-upload-path) "/" target-name) convert-format)
        (response/ok {:path target-name})))))

(def mainCsv (csvHandler tools/convert-data "data-main.csv"))
(def frCsv (csvHandler tools/fr-data "data-fr.csv"))
(def esCsv (csvHandler tools/es-data "data-es.csv"))
(def deCsv (csvHandler tools/de-data "data-de.csv"))


(defroutes arikami-routes
  (GET "/arikami/import" []
       (import-page))
  (POST "/arikami/upload-file" [excelfile]
        (upload-file (get-upload-path) excelfile)
        (response/found "/arikami/import"))
  (POST "/arikami/verifyImages" [filename]
        (verifyImages filename))
  (POST "/arikami/verifySku" [filename]
        (verifySku filename))
  (POST "/arikami/checkExcel" [filename]
        (checkExcel filename))
  (POST "/arikami/importAttr" [filename]
        (importAttr filename))
  (POST "/arikami/delIndex" [filename]
        (delIndex filename))
  (POST "/arikami/importAttrTest" [filename]
        (importAttrTest filename))
  (POST "/arikami/delIndexTest" [filename]
        (delIndexTest filename))
  (POST "/arikami/mainCsv" [filename]
        (mainCsv filename))
  (POST "/arikami/frCsv" [filename]
        (frCsv filename))
  (POST "/arikami/esCsv" [filename]
        (esCsv filename))
  (POST "/arikami/deCsv" [filename]
        (deCsv filename))
  (POST "/arikami/reindex" [filename]
        (reIndex filename))
  (POST "/arikami/reindextest" [filename]
        (reIndexTest filename))
  (POST "/arikami/flushcache" [filename]
        (flushCache filename))
  (POST "/arikami/flushcacheTest" [filename]
        (flushCacheTest filename))
  (GET "/file/:filename" [filename]
       (response/file-response (str (get-upload-path) "/" filename))))
