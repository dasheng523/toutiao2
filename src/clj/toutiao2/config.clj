(ns toutiao2.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [clojure.string :as str]
            [mount.core :refer [args defstate]]))

(defstate env :start (load-config
                       :merge
                       [(args)
                        (source/from-system-props)
                        (source/from-env)]))


(def cos
  {:name (System/getProperty "os.name"),
   :version (System/getProperty "os.version"),
   :arch (System/getProperty "os.arch")})

(defn isWindows? []
  (str/includes? (str/lower-case (:name cos)) "window"))

(defn isMac? []
  (str/includes? (str/lower-case (:name cos)) "mac"))

(defn get-download-path []
  (if (isWindows?)
    (-> env :win-download-path)
    (-> env :mac-download-path)))

(defn get-cookies-path []
  (if (isWindows?)
    (-> env :win-cookies-path)
    (-> env :mac-cookies-path)))
