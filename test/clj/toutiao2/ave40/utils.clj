(ns toutiao2.ave40.utils
  (:require [net.cgrand.enlive-html :as enlive]
            [clojure.string :as str]))

(defn lazy-contains? [col key]
  (not (empty? (filter #(= key %) col))))

(defn get-html-node-text [nodes]
  (map (fn [n]
         (str/trim (enlive/text n))) nodes))

(defn get-domain [url]
  (-> (str/split url #"/")
      (#(str/join "/" [(get % 0) (get % 1) (get % 2)]))))
