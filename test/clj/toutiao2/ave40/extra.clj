(ns toutiao2.ave40.extra
  [:require [dk.ative.docjure.spreadsheet :as sheet]
            [clojure.data.json :as json]
            [net.cgrand.enlive-html :as enlive]
            [clojure.data :as data]
            [clojure.string :as str]
            [clj-http.client :as http]
            [toutiao2.ave40.db :refer :all]
            [toutiao2.ave40.utils :refer :all]
            [toutiao2.ave40.article :as article]
            [toutiao2.ave40.grap-article :as grap]
            [clojure.java.io :as io]
            [clojure.walk :as w]]
  (:import (java.io StringReader)))

(defn- parse-excel-data [list]
  (cons (map name (keys (first list))) (map #(vals %) list)))

#_(parse-excel-data (select-all article-db {:table "cms_block" :cols ["block_id" "title" "content"]}))

(defn save-to-excel
  ([data]
   (let [wb (sheet/create-workbook "html" (parse-excel-data data))]
     (sheet/save-workbook! "d:/exponents.xlsx" wb)))
  ([data file]
   (let [wb (sheet/create-workbook "html" (parse-excel-data data))]
     (sheet/save-workbook! file wb))))

(defn list-dir [path]
  (file-seq (io/file path)))

(defn- split-trim [s]
  (str/split s #"\n|\t+")
  #_(filter not-empty (map (fn [pie]
                 (let [temp (str/trim pie)]
                   (if-not (empty? temp) temp))) (str/split s #"\n"))))

(defn run-extra []
  (let [grapper (grap/simple-grapper
                  (grap/create-default-selector [:div.node :h2.title :a] "http://www.autoexpress.co.uk")
                  #(str "http://www.autoexpress.co.uk/car-news/page/" % "/0"))]
    (future (grapper 1 200))
    (future (grapper 200 400))
    (future (grapper 400 600))
    (future (grapper 600 860))))

(defn do-parse-article []
  (article/do-parse-and-save {:domain "https://www.vapingpost.com"
                      :selector {:title [:header :> :h1.entry-title]
                                 :article [:div.td-ss-main-content :> :div.td-post-content]}
                      :cond "html like '%entry-title%' and html like '%td-post-content%'"}))


(defn find-all-file [dir-path]
  (let [dir-seq (file-seq (io/file dir-path))
        phtml-list (filter #(and (.isFile %)
                                 (.endsWith (.getName %) ".phtml")) dir-seq)
        ready-list (-> (slurp "D:\\Ave40_Translate.csv")
                 (str/split #"\r\n")
                 (#(map (fn [s]
                          (first (str/split (str/replace s #"\"" "") #","))) %)))]
    (save-to-excel
      (into #{} (remove #(and (nil? %) (lazy-contains? ready-list %))
                        (for [item phtml-list]
                          (if-let [ffind (re-find #"\$this->__\((['\"])([^\n]+?)\1\)" (slurp item))]
                            (conj (zipmap ["source" "quoti" "value"] ffind)
                                  {"file" (.getAbsolutePath item)}))))))))

#_(find-all-file "E:\\ave40_mg\\app\\design\\frontend\\default\\se105")

(defn- get-ready-translate-list []
  (-> (slurp "D:\\Ave40_Translate.csv")
      (str/split #"\r\n")
      (->> (map #(first (str/split (str/replace % #"\"" "") #","))))
      (->> (into #{}))))


(defn find-match-file [path]
  (let [phtml-list (-> (list-dir path)
                       (->> (filter #(.endsWith (.getName %) ".php"))))
        ready-list (get-ready-translate-list)]
    (->> phtml-list
         (mapcat #(re-seq #"Mage::helper\(\"Ave40_Translate\"\)->__\((['\"])([^\n\$]+?)\1\)" (slurp %)))
         #_(map #(get % 2))
         (into #{})
         #_(remove #(or (nil? %) (lazy-contains? ready-list (get % 2)))))))


(defn save-match-data [data]
  (let [wb (sheet/create-workbook "html" data)]
    (sheet/save-workbook! "d:/data.xlsx" wb)))

#_(-> "E:\\ave40_mg\\app\\design\\frontend\\default\\se105"
    find-match-file
    save-match-data)

#_(-> "E:\\ave40_mg\\app\\code\\local\\Ave40"
    find-match-file
    save-match-data)



(defn replace-all-file [dir-path]
  (let [dir-seq (file-seq (io/file dir-path))
        phtml-list (filter #(and (.isFile %)
                                 (.endsWith (.getName %) ".php")
                                 (not (.contains (.getAbsolutePath %) "Adminhtml"))) dir-seq)]
    (doseq [item phtml-list]
      (spit (.getAbsolutePath item)
            (str/replace (slurp item) #"\$this->__" "Mage::helper(\"Ave40_Translate\")->__")))))


#_(replace-all-file "E:\\ave40_mg\\app\\code\\local\\Ave40")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;  导入产品数据


(defn copy-images []
  (let [dir-seq (file-seq (io/file "E:\\fayusource"))
        phtml-list (filter #(and (.isFile %)
                                 (or (.endsWith (.getName %) ".jpg")
                                     (.endsWith (.getName %) ".gif")
                                     (.endsWith (.getName %) ".png"))) dir-seq)]
    (doseq [file phtml-list]
      (let [filename (-> (.getAbsolutePath file)
                         (str/split #"\\")
                         (get 2)
                         (str/replace #" " "-")
                         (subs 0 10)
                         (str "_" (.getName file)))]
        (println filename)
        (io/copy file (io/file (str "E:\\fayuimages\\" filename)))))))

#_(copy-images)

(defn- do-one-html [file]
  (let [content (slurp file :encoding "gb2312")
        filename (.getName file)
        incontent (-> content
                      (StringReader.)
                      (enlive/html-resource)
                      (enlive/select [:div.WordSection1])
                      (first)
                      :content
                      (enlive/emit*)
                      (->> (apply str)))
        piclist (-> incontent
                    (->> (re-seq #"<img[^<>]*src=\"(.*?)\"[^<>]*>"))
                    (->> (map #(get % 1))))]
    (spit (str "E:\\fayuhtml\\" filename)
          (reduce (fn [s picurl]
                    (let [newpicurl (-> (str/replace picurl #"%20" "-")
                                        (subs 0 10)
                                        (str "_" (second (str/split picurl #"/")))
                                        (->> (str "http://www.demo.ave40.com/media/Frenchimages/")))]
                      (str/replace s picurl newpicurl))) incontent piclist))))
(defn copy-htmls []
  (let [dir-seq (file-seq (io/file "E:\\fayusource"))
        phtml-list (filter #(and (.isFile %)
                                 (.endsWith (.getName %) ".htm")) dir-seq)]
    (doseq [file phtml-list]
      (do-one-html file))))

#_(copy-htmls)

(def ave40-db
  {:classname "com.mysql.jdbc.Driver"
   :subprotocol "mysql"
   :subname "//127.0.0.1/ttttt"
   :user "root"
   :password "a5235013"
   :characterEncoding "UTF-8"
   :sslmode "require"})

(def ave40-db-true
  {:classname "com.mysql.jdbc.Driver"
   :subprotocol "mysql"
   :subname "//127.0.0.1/ave40_mg"
   :user "root"
   :password "a5235013"
   :characterEncoding "UTF-8"
   :sslmode "require"})

(defn- find-product-id [s]
  (let [sku (-> (str/replace s #"_" "-")
                (str/split #"-")
                (second))]
    (if sku (:entity_id (select-one ave40-db {:table "catalog_product_entity"  :where (str "sku='" sku "'")})))))

#_(find-product-id "FR-1080-Wismec_Predator_228W_TC_Kit_with_Elabo")

(defn- insert-product-data []
  (doseq [file (filter #(.endsWith (.getName %) ".htm")
                       (file-seq (io/file "E:\\fayuhtml")))]
    (let [filename (.getName file)
          filecontent (slurp file)
          id (find-product-id filename)
          name (-> filecontent
                   (StringReader.)
                   (enlive/html-resource)
                   (enlive/select [#{:p.LO-Normal :p.MsoNormal} :b :span])
                   (first)
                   (enlive/text)
                   (str/replace #"\n" " "))
          desc-data {"entity_type_id" 4
                     "attribute_id" 72
                     "store_id" 4
                     "entity_id" id
                     "value" filecontent}
          title-data {"entity_type_id" 4
                      "attribute_id" 71
                      "store_id" 4
                      "entity_id" id
                      "value" name}]
      (when id
        (try
          (println [id filename])
          (insert-table-data ave40-db {:table "catalog_product_entity_text"
                                       :cols (keys desc-data) :vals (vals desc-data)})
          (insert-table-data ave40-db {:table "catalog_product_entity_varchar"
                                       :cols (keys title-data) :vals (vals title-data)})
          (catch Exception e
            (println (str "error: " [id filename]))))))))


(defn- read-csv [path]
  (-> path
      slurp
      (str/split #"\r\n")
      (->> (remove #(= "" %)))
      (->> (map #(str/split (subs % 1 (- (count %) 1)) #"\",\"")))))

(defn- read-excel [path cols]
  (->> path
       (sheet/load-workbook)
       (sheet/select-sheet "html")
       (sheet/select-columns cols)))

(defn- load-keyword-data [path]
  (read-excel path {:A :keyword}))

(defn- load-block-keyword-data []
  (let [data (select-all ave40-db {:table "cms_block"})]
    (map #(last %) (mapcat
               (fn [{:keys [content block_id]}]
                 (re-seq #"\{\{t t=\"(.+?)\"\}\}" content))
               data))))

#_(load-block-keyword-data )


(defn- all-keywords-data []
  (let [app-path "D:\\fayu\\app.xlsx"
        view-path "D:\\fayu\\view.xlsx"
        app-data (load-keyword-data app-path)
        view-data (load-keyword-data view-path)
        need-data (map (fn [n] {:keyword (first n)}) (read-csv "D:\\fayu\\Ave40_Translate.csv"))
        block-data (map (fn [n] {:keyword n}) (load-block-keyword-data))]
    (into #{} (concat app-data view-data need-data block-data))))


(defn- read-txt [path]
  (-> path
      slurp
      (str/split #"\r\n")
      (->> (remove #(= "" %)))
      (->> (map (fn [n] (into [] (map #(str/trim %) (str/split n #":"))))))))

(defn- read-trans-excel [path]
  (-> (read-excel path {:A :keyword, :B :value})
      (->> (map (fn [{:keys [keyword value]}] [keyword value])))))


(defn- all-translate-data []
  (let [csv (read-csv "D:\\fayu\\trans.csv")
        txt (read-txt "D:\\fayu\\ttt.txt")
        excel (read-trans-excel "D:\\fayu\\trans.xlsx")]
    (into {} (concat csv txt excel))))


(defn- parse-csv-str [data]
  (reduce
    (fn [s {:keys [keyword value]}]
      (let [re-keyword (str/replace keyword #"\"" "\\\"")
            re-value (str/replace value #"\"" "\\\"")]
        (str s "\"" re-keyword "\",\"" re-value "\"\n")))
    ""
    data))

(defn- parse-result-translate-file []
  (let [keywords (all-keywords-data)
        translate (all-translate-data)
        data (for [{:keys [keyword]} keywords]
               (if (get translate keyword)
                 {:keyword keyword :value (get translate keyword)}
                 {:keyword keyword :value keyword}))
        group-data (group-by #(= (:keyword %) (:value %)) data)]
    (-> (get group-data true)
        parse-csv-str
        (->> (spit "D:\\fayu\\rs-some.csv")))
    (-> (get group-data false)
        parse-csv-str
        (->> (spit "D:\\fayu\\rs-diff.csv")))))

#_(parse-result-translate-file)


(defn find-text-in-htmlnodes [nodes]
  (reduce
    (fn [coll node]
      (let [content (:content node)]
        (into #{}
              (if (some #(and (string? %) (not= "" (str/trim %))) content)
                (conj coll (-> content (enlive/emit*) (str/join)))
                (concat coll (find-text-in-htmlnodes content)))))) [] nodes))


(defn change-to-nodes [content]
  (-> content
      (str/replace #"\r\n" "")
      (StringReader.)
      (enlive/html-resource)))

(defn- find-tmp-text []
  (-> (slurp "D:\\fayu\\in-tmp.tmp")
      (change-to-nodes)
      (find-text-in-htmlnodes)))

(defn- replace-tmp-text [keywords]
  (let [content (slurp "D:\\fayu\\in-tmp.tmp")]
    (spit "D:\\fayu\\out-tmp.tmp"
          (reduce #(str/replace %1 %2 (str "{{t t=\"" %2 "\"}}")) content keywords))))


(defn create-page-data [name]
  (let [info (select-one ave40-db-true
                         {:table "cms_page" :where (str "is_active=1" " and " "title='" name "'")})
        page-id (:page_id info)
        in-info (w/stringify-keys (dissoc info :page_id))]
    (insert-table-data ave40-db-true
                       {:table "cms_page_store"
                        :cols ["page_id" "store_id"]
                        :vals [page-id 1]})
    (let [new-page-id (-> (insert-table-data
                            ave40-db-true
                            {:table "cms_page" :cols (keys in-info) :vals (vals in-info)})
                          :generated_key)]
      (insert-table-data ave40-db-true
                         {:table "cms_page_store" :cols ["page_id" "store_id"] :vals [new-page-id 4]}))))



(defn ddd []
  (create-page-data "More About AVE40 | Buy Electronic Cigarette Online, Buy Original E-Cigs at AVE40")
  (create-page-data "Free Membership Agreement")
  (create-page-data "Affiliate Program")
  (create-page-data "BBS Marketing Program")
  (create-page-data "cart")
  (create-page-data "Clearance Sale For E-Cigarette")
  (create-page-data "Company")
  (create-page-data "Contact Us")
  (create-page-data "Customer Service")
  (create-page-data "details")
  (create-page-data "drop-shipping")
  (create-page-data "Enable Cookies")
  (create-page-data "Get your free website now!")
  (create-page-data "Privacy Policy")
  (create-page-data "Welcome to our Exclusive Online Store")
  (create-page-data "Reward Points")
  (create-page-data "Rewards")
  (create-page-data "503 Service Unavailable")
  (create-page-data "Share for Discounts")
  (create-page-data "Shipping & Tracking")
  (create-page-data "Terms & Conditions")
  (create-page-data "vaporesso-sale")
  (create-page-data "Warranty & Returns")
  (create-page-data " Wholesale"))

#_(ddd)

(defn create-block-data [identifier]
  (let [info (select-one ave40-db-true
                         {:table "cms_block" :where (str "is_active=1" " and " "identifier='" identifier "'")})
        block_id (:block_id info)
        in-info (w/stringify-keys (dissoc info :block_id))]
    (insert-table-data ave40-db-true
                       {:table "cms_block_store"
                        :cols ["block_id" "store_id"]
                        :vals [block_id 1]})
    (let [new-block-id (-> (insert-table-data
                             ave40-db-true
                            {:table "cms_block" :cols (keys in-info) :vals (vals in-info)})
                          :generated_key)]
      (insert-table-data ave40-db-true
                         {:table "cms_block_store" :cols ["block_id" "store_id"] :vals [new-block-id 4]}))))

(defn do-create-block []
  (create-block-data "home-bottom-fix")
  (create-block-data "home-four-service")
  (create-block-data "new-navigation")
  (create-block-data "new-navigation-front")
  (create-block-data "footer-customer-services")
  (create-block-data "newhome-bast-seller")
  (create-block-data "newhome-newarrival")
  (create-block-data "newhome-about-us-content")
  (create-block-data "product_logis")
  (create-block-data "product_aftersale")
  (create-block-data "product_shipping_and_payment")
  (create-block-data "home-search-quick")
  (create-block-data "home-blog-article")
  (create-block-data "home-product-list-1a")
  (create-block-data "home-product-list-2b")
  (create-block-data "home-product-list-3c")
  (create-block-data "home-product-list-4d")
  (create-block-data "home-product-list-5f")
  (create-block-data "product_safe"))



#_(do-create-block)

#_(find-tmp-text)

#_(replace-tmp-text [" <br /> <strong>How to Join our Wholesale Program</strong><br /> <br /> Send us an email of inquiry to <a href=\"mailto:info@ave40.com\">info@ave40.com</a>. One of our stuffers will contact you for placing a wholesale order offline.<br /> You can also set up a wholesale account and navigate through the whole online ordering system. Click our Wholesale Guide <a href=\"https://www.ave40.com/wholesale/wholesale-guide.html\">https://www.ave40.com/wholesale/wholesale-guide.html</a> to know how.<br /> If you have any questions concerning all these, please drop us an email to <a href=\"mailto:support@ave40.com\">support@ave40.com</a>. Our staffers will resolve them for you in time.<br /> <br />  <br /> Last but not least, occasionally we have rounds of sales promotion for consumers and wholesale partners, so stay posted!<br /> <br />  <br /> <span class=\"inquireNow-popup-btn inquireNow\">Wholesale Inquiry</span>"
                   "Demo samples upon launching of new products on a case-to-case basis;"
                   "Reliable DHL/UPS/FedEx courier service."
                   "<br /> <br /> <br /> Thank you and welcome to join our Wholesale Program!<br />  <br /> Years of industrial presence and sound distributorships have allowed us to pass the additional value of our goods and services on to our wholesale partners, here defined as retailers, vapor shops, wholesalers, and industrial, commercial, and professional business owners.<br /> <br /> <strong>What We Provide</strong>"
                   "Various discounts throughout our product lines for your bulk and/or wholesale orders;"
                   "Customized quotations for your orders depending on the order size, frequency, and lifetime value of the partnership;"
                   "Same day or next day delivery for stocked products;"])




#_(-> (slurp "D:\\fayu\\in-tmp.tmp")
    change-to-nodes)

#_(-> (select-all ave40-db {:table "cms_page" :where "page_id not in (1,2,3)"})
    (->> (mapcat (fn [info]
                   (-> info
                       :content
                       (StringReader.)
                       (enlive/html-resource)
                       (->> (map #(-> (enlive/text %) (str/trim))))
                       (->> (mapcat #(str/split % #"\n")))
                       (->> (mapcat #(str/split % #"\/")))
                       (->> (mapcat #(str/split % #"  \|  ")))
                       (->> (map (fn [s] (str/trim s))))
                       (->> (remove #(= "" %)))
                       (->> (map (fn [x] {:words x :block_id (:block_id info)})))))))
    #_(save-to-excel))


#_(let [data (->> (sheet/load-workbook "f:\\blocks.xlsx")
                (sheet/select-sheet "Sheet1")
                (sheet/select-columns {:A :block_id, :B :content :C :fr}))]
  (doseq [{:keys [block_id content fr]} data]
    (let [id (int block_id)
          mydata (select-one ave40-db {:table "cms_block" :where (str "block_id=" id)})
          my-content (:content mydata)
          new-content (str/replace my-content content (str "{{t t=\"" content "\"}}"))]
      (update-data ave40-db {:table "cms_block"
                             :where (str "block_id=" block_id)
                             :updates {:content new-content}}))))


#_(let [data (->> (sheet/load-workbook "f:\\blocks.xlsx")
                (sheet/select-sheet "Sheet1")
                (sheet/select-columns {:A :block_id, :B :content :C :fr})
                (map #(str "\"" (:content %) "\"," "\"" (:fr %) "\""))
                (str/join "\n"))]
  (spit "f:\\ddd.csv" data))
