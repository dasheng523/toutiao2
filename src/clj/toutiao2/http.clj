(ns toutiao2.http
  (:require [toutiao2.utils :as utils]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]

            [clojure.string :as str]))

(def default-user-agent "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.162 Safari/537.36")


(defn create-default-header [host cookies]
  {"User-Agent" default-user-agent
   "Cookie" cookies
   "Accept" "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"
   "Accept-Encoding" "gzip, deflate, br"
   "Accept-Language" "zh-CN,zh;q=0.9,fr;q=0.8"
   "Cache-Control" "max-age=0"
   "Connection" "keep-alive"
   "Host" host
   "Upgrade-Insecure-Requests" 1
   })

(defn- read-host-from-url [url]
  (nth (str/split url #"/") 2))


(defn compose-http
  ([method done? failback]
   (let [dofn (if (= method :get)
                http/get
                http/post)]
     (fn [url params]
       (utils/retry #(dofn url params)
                    done?
                    3
                    failback))))
  ([method]
   (compose-http method
                 #(= 200 (:status %))
                 #(log/error %))))

(defn compose-get
  [cookies done? failback]
  (let [dofn http/get]
    (fn [url]
      (utils/retry #(dofn url {:headers (create-default-header
                                         (read-host-from-url url)
                                         cookies)})
                   done?
                   3
                   failback))))


(def simple-get (fn [url cookie]
              ((compose-http :get)
               url
               {:headers (create-default-header
                          (read-host-from-url url)
                          cookie)})))

(def simple-post (fn [url params cookie]
               ((compose-http :post)
                url
                {:headers (create-default-header
                           (read-host-from-url url)
                           cookie)
                 :form-params params})))

#_(println (simple-post "http://cgkc.admin/api/inquire" {:type 1 :phone 3}))
#_(println (simple-get "https://www.baidu.com"))
