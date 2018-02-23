(ns toutiao2.routes.arikami
  (:require [toutiao2.layout :as layout]
            [toutiao2.config :refer [env]]
            [toutiao2.utils :as utils]
            [toutiao2.arikami.tools :as tools]
            [compojure.core :refer [defroutes GET POST]]
            [selmer.filters :refer [add-filter!]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io File FileInputStream FileOutputStream]
           [java.nio.file Files LinkOption]
           [java.nio.file.attribute BasicFileAttributes]))

(def upload-path (-> env :upload-path))


(defn file-attributes [file]
  "Return the file BasicFileAttributes of file.  File can be a file or a string
   (or anything else acceptable to jio/file)"
  (Files/readAttributes (.toPath (io/file file))
                        BasicFileAttributes
                        (into-array LinkOption [])))

(defn upload-file-name []
  (map #(.getName %)
       (some-> upload-path
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



(defn verifyImages [filename]
  (try
    (let [data (-> (file-path upload-path filename)
                   (utils/read-excel->map "upload_template"))
          rs {:error-images (->> data
                                 (mapcat #(str/split (:image %) #";"))
                                 (set)
                                 (tools/verify-urls)
                                 (remove nil?))}]
      (response/ok rs))
    (catch Exception e
      {:error-msg "file content error"})))

(defn verifySku [filename]
  (try
    (let [data (-> (file-path upload-path filename)
                   (utils/read-excel->map "upload_template"))
          rs {:error-sku (->> data
                              (tools/verify-sku)
                              (remove nil?))}]
      (response/ok rs))
    (catch Exception e
      {:error-msg "file content error"})))

(defn importAttr [filename]
  (try
    (let [data (-> (str upload-path "/" filename)
                   (utils/read-excel->map "upload_template"))]
      (tools/init-attribute-options (tools/attribute-dataset data)))
    (catch Exception e
      {:error-msg "file content error"})))

(defn delIndex [_]
  (try
    (tools/delIndex)
    (catch Exception e
      {:error-msg "file content error"})))

(defn mainCsv [filename]
  (try
    (let [data (-> (str upload-path "/" filename)
                   (utils/read-excel->map "upload_template"))]
      (tools/do-all-data-logic data (str upload-path "/" "data-main.csv") tools/convert-data)
      (response/ok {:path "data-main.csv"}))
    (catch Exception e
      {:error-msg "file content error"})))

(defn frCsv [filename]
  (try
    (let [data (-> (str upload-path "/" filename)
                   (utils/read-excel->map "upload_template"))]
      (tools/do-all-data-logic data (str upload-path "/" "data-fr.csv") tools/fr-data)
      (response/ok {:path "data-fr.csv"}))
    (catch Exception e
      {:error-msg "file content error"})))

(defn esCsv [filename]
  (try
    (let [data (-> (str upload-path "/" filename)
                   (utils/read-excel->map "upload_template"))]
      (tools/do-all-data-logic data (str upload-path "/" "data-es.csv") tools/es-data)
      (response/ok {:path "data-es.csv"}))
    (catch Exception e
      {:error-msg "file content error"})))

(defn deCsv [filename]
  (try
    (let [data (-> (str upload-path "/" filename)
                   (utils/read-excel->map "upload_template"))]
      (tools/do-all-data-logic data (str upload-path "/" "data-de.csv") tools/de-data)
      (response/ok {:path "data-de.csv"}))
    (catch Exception e
      {:error-msg "file content error"})))


(defroutes arikami-routes
           (GET "/arikami/import" []
             (import-page))
           (POST "/arikami/upload-file" [excelfile]
             (upload-file upload-path excelfile)
             (response/found "/arikami/import"))
           (POST "/arikami/verifyImages" [filename]
             (verifyImages filename))
           (POST "/arikami/verifySku" [filename]
             (verifySku filename))
           (POST "/arikami/importAttr" [filename]
             (importAttr filename))
           (POST "/arikami/delIndex" [filename]
             (delIndex filename))
           (POST "/arikami/mainCsv" [filename]
             (mainCsv filename))
           (POST "/arikami/frCsv" [filename]
             (frCsv filename))
           (POST "/arikami/esCsv" [filename]
             (esCsv filename))
           (POST "/arikami/deCsv" [filename]
             (deCsv filename))
           (GET "/file/:filename" [filename]
             (response/file-response (str upload-path "/" filename))))
