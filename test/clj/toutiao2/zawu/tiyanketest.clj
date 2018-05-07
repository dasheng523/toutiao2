(ns toutiao2.zawu.tiyanketest
  (:require  [clojure.test :as t]
             [clj-http.client :as http]))


(defn get-list-data [kword city cateId page]
  (let [pagesize 32
        offset (* page pagesize)
        url (str "http://apimobile.meituan.com/group/v4/poi/pcsearch/" city "?uuid=4d768a970e22495f992d.1525314072.1.0.0&userid=-1&limit=" limit "&offset=" offset "&cateId=" cateId "&q=" kword)]
    (some-> url
            (http/get {:headers {"User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.162 Safari/537.36"
                                 "Cookie" "uuid=4d768a970e22495f992d.1525314072.1.0.0; __mta=41884189.1525314095769.1525314095769.1525314095769.1; ci=30; rvct=30; _lxsdk_cuid=163383b86ddc8-0f4f2624a7c373-3961430f-1fa400-163383b86ddc8; _lxsdk_s=163383b86df-d72-47f-b42%7C%7C2"}
                       :debug true})
            :body
            )))



(http/get "http://apimobile.meituan.com/group/v4/poi/pcsearch/30?uuid=4d768a970e22495f992d.1525314072.1.0.0&userid=-1&limit=32&offset=64&cateId=-1&q=体验课"
          {:headers {"User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.162 Safari/537.36"
                     "Cookie" "uuid=4d768a970e22495f992d.1525314072.1.0.0; __mta=41884189.1525314095769.1525314095769.1525314095769.1; ci=30; rvct=30; _lxsdk_cuid=163383b86ddc8-0f4f2624a7c373-3961430f-1fa400-163383b86ddc8; _lxsdk_s=163383b86df-d72-47f-b42%7C%7C2"}
           :debug true})

"http://apimobile.meituan.com/group/v4/poi/pcsearch/10?uuid=4d768a970e22495f992d.1525314072.1.0.0&userid=-1&limit=32&offset=32&cateId=-1&q=体验课"
"http://www.meituan.com/deal/43985172.html"
