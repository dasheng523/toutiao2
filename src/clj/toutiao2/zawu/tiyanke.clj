(ns toutiao2.zawu.tiyanke
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(def cookies "uuid=4d768a970e22495f992d.1525314072.1.0.0; __mta=41884189.1525314095769.1525314095769.1525314095769.1; ci=30; rvct=30; _lxsdk_cuid=163383b86ddc8-0f4f2624a7c373-3961430f-1fa400-163383b86ddc8; _lxsdk_s=163383b86df-d72-47f-b42%7C%7C2")
(def user-agent "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.162 Safari/537.36")

(defn get-list-data [kword city cateId areaId page]
  (let [pagesize 32
        offset (* page pagesize)
        url (str "http://apimobile.meituan.com/group/v4/poi/pcsearch/" city "?uuid=4d768a970e22495f992d.1525314072.1.0.0&userid=-1&limit=" pagesize "&offset=" offset "&cateId=" cateId "&q=" kword "&areaId=" areaId)]
    (some-> url
            (http/get {:headers {"User-Agent" user-agent
                                 "Cookie" cookies}
                       :debug true})
            :body
            (json/parse-string true))))

(defn get-detail-data [id]
  (let [url (str "http://www.meituan.com/deal/" id ".html")
        result-html (-> (http/get url {:headers {"User-Agent" user-agent
                                                 "Cookie" cookies}})
                        :body)
        imgurl (some-> (re-find #"\"imageText\":(.*?),\"navbar\"" result-html)
                       second
                       (json/parse-string true)
                       (->> (map :image)))]
    {:image imgurl}))

#_(get-detail-data 43985172)

#_(get-list-data "体验课" 10 -1 6 1)
