(ns toutiao2.arikami.tools
  (:require [toutiao2.utils :as utils]
            [toutiao2.arikami.db :as db]
            [clj-time.local :as l-t]
            [clojure.string :as str]
            [clj-http.client :as http]
            [clojure.spec.alpha :as s]
            [clojure.walk :as walk]
            [ring.util.codec :as codec]
            [clojure.java.io :as io]
            [clojure.set :as cset]
            [clojure.java.shell :as shell]
            [toutiao2.config :refer [env]]))

(def import-field
  [:use_config_max_sale_qty :description :thumbnail_image :bundle_sku_type :custom_design_from
   :manage_stock :custom_design_to :small_image_label :use_config_enable_qty_inc :associated_skus
   :map_enabled :swatch_image :bundle_shipment_type :enable_qty_increments :new_from_date
   :crosssell_skus :special_price :configurable_variations :website_id :meta_title :name
   :use_config_min_qty :gift_message_available :min_cart_qty :related_skus :country_of_manufacture
   :use_config_backorders :bundle_price_type :custom_design :product_online :qty_increments
   :swatch_image_label :notify_on_stock_below :display_product_options_in :upsell_position
   :meta_keywords :short_description :msrp_price :product_options_container :use_config_manage_stock
   :product_type :hide_from_product_page :sku :related_position :updated_at :base_image
   :additional_images :tax_class_name :is_qty_decimal :out_of_stock_qty :bundle_weight_type
   :categories :product_websites :special_price_to_date :weight :max_cart_qty :use_config_qty_increments
   :additional_image_labels :meta_description :map_price :crosssell_position :qty
   :additional_attributes :is_decimal_divided :url_key :custom_layout_update :bundle_price_view
   :thumbnail_image_label :special_price_from_date :allow_backorders :use_config_min_sale_qty
   :small_image :is_in_stock :use_config_notify_stock_qty :price :new_to_date :visibility
   :attribute_set_code :configurable_variation_labels :store_view_code :msrp_display_actual_price_type
   :custom_options :upsell_skus :created_at :base_image_label :page_layout])

(def empty-product-info (reduce #(assoc %1 %2 "") {} import-field))

(def default-product-info
  {:use_config_max_sale_qty 1
   :manage_stock 1
   :use_config_enable_qty_inc  0
   :enable_qty_increments      0
   :website_id                 0
   :use_config_min_qty         1
   :min_cart_qty               1.0000
   :use_config_backorders      1
   :product_online             1
   :qty_increments             0.0000
   :notify_on_stock_below      1
   :display_product_options_in "Block after Info Column"
   :use_config_manage_stock    1
   :allow_backorders           1
   :tax_class_name             "Taxable Goods"
   :is_qty_decimal             0
   :out_of_stock_qty           0.0000
   :product_websites           "base"
   :attribute_set_code "Default"
   :max_cart_qty               1000
   :use_config_qty_increments  1
   :qty 1000
   :is_decimal_divided 0
   :use_config_min_sale_qty 1
   :is_in_stock 1
   :use_config_notify_stock_qty 1
   :updated_at                 (l-t/local-now)
   :created_at (l-t/local-now)
   :product_type "simple"
   :weight 0})

(defn trim-map-val [m]
  (reduce (fn [rs k] (update rs k #(if (string? %) (str/trim %) %))) m (keys m)))


(defn- name->url-key [name idstr]
  (-> name
      (str "-" idstr)
      (str/lower-case)
      (str/replace #"=" "-")
      (str/replace #"," "-")
      (str/replace #"/" "-")
      (str/replace #" " "-")
      (str/replace #"\." "")
      (str/replace #"\+" "")
      (codec/url-encode)))

(defn- sub-products [sku list]
  (filter #(and (str/starts-with? (:sku %) sku) (not= sku (:sku %))) list))

(defn- parent-product [sku list]
  (-> (filter #(= (:sku %) (-> sku (str/split #"-") first)) list)
      (first)))


(defn- format-attr [s]
  (if s
    (-> (if (number? s) (str (int s)) s)
        (str/replace #"," " -")
        (str/trim))))

(defn get-attrs [data]
  (-> {"color" (-> (:color data) format-attr)
       "material" (-> (:material data) format-attr)
       "size" (-> (:size data) format-attr)
       "type" (-> (:type data) format-attr)
       "capacity" (-> (:capacity data) format-attr)
       "option" (-> (:option data) format-attr)}
      (->> (filter second)
           (into {}))))


(defn maps->string-format [m]
  (->> m
       (map #(str (-> % first str (str/trim)) "=" (-> % second str (str/trim))))
       (str/join ",")))

(defn convert-addattr [data]
  (-> (get-attrs data)
      (maps->string-format)))


(defn genereate-related-skus [data]
  (str/replace (str (:related_skus data))  #";" ","))


(defn generate-description
  ([data]
   (generate-description data :description_en))
  ([data desc-key]
   (let [img-str (if (string? (:des_image data))
                   (-> (str/split (:des_image data) #";")
                       (->> (map #(str "http://www.arikami.com/media/Products/" %)))
                       (->> (map #(str "<img src='" % "'>")))
                       (->> (str/join "\n"))))
         desc (if (string? (desc-key data))
                (-> (desc-key data)
                    (str/split #"\n")
                    (->> (map #(str "<p>" % "</p>"))
                         (str/join "\n"))))]
     (str desc "\n" img-str))))



(defn convert-configvar [data list]
  (if (= (:product_type data) "configurable")
    (let [sku (:sku data)
          f-list (sub-products sku list)
          config-map (map #(merge (get-attrs %) {"sku" (:sku %)}) f-list)]
      (when (not-empty config-map)
        (->> config-map
             (map maps->string-format)
             (str/join "|"))))))

(defn convert-additional_images [item list]
  (let [top-product (parent-product (:sku item) list)]
    (-> (:image (if (and (= (:product_type item) "simple")
                         (not-empty top-product))
                  top-product
                  item))
        (str/replace #";" ",")
        (str/replace #"&" ""))))


(defn attribute-dataset
  "导出所有属性值"
  [list]
  (reduce
   (fn [m item]
     (reduce (fn [v k]
               (update v k #(conj % (-> (get item k) format-attr))))
             m
             (keys m)))
   {:color #{} :material #{} :size #{} :type #{} :capacity #{} :option #{}}
   list))

(def attribute-map {:color 90
                    :size 134
                    :type 144
                    :capacity 145
                    :option 146
                    :material 147})


(defn convert-images [data list]
  (let [image (and (:image data)
                   (-> (:image data)
                       (str/split #";")
                       first
                       (str/replace #"&" "")))]
    {:base_image image
     :thumbnail_image image
     :small_image image
     :additional_images (and (not-empty (:image data))
                             (not-empty (:product_type data))
                             (convert-additional_images data list))}))



(defn find-sku [sku list]
  (-> list
      (->> (filter #(= sku (:sku %))))
      (first)))


(s/check-asserts true)
(s/def :data/name_en string?)
(s/def :data/price number?)
(s/def :data/image string?)
(s/def :data/sku string?)
(s/def :data/categories string?)
(s/def :data/product_type (s/and string? #(some #{%} ["configurable" "simple"])))
(s/def :data/weight number?)
(s/def ::data
  (s/keys :req-un
          [:data/name_en
           :data/price
           :data/image
           :data/sku
           :data/categories
           :data/product_type
           :data/weight]))
(s/def ::data-list
  (s/coll-of ::data))


(defn convert-data [data list]
  (merge {:sku (:sku data)
          :name (:name_en data)
          :price (:price data)
          :special_price (:special_price data)
          :description (generate-description data)
          :categories (and (:categories data)
                           (-> (:categories data)
                               (str/replace #",Categories" ";Categories")
                               (str/replace #", Categories" ";Categories")
                               (str/replace #", " " ")
                               (str/replace #"," " ")
                               (str/replace #";" ",")))
          :product_type (:product_type data)
          :meta_title (:meta_title data)
          :meta_keywords (if (empty? (:meta_keywords data))
                           (-> (name->url-key (get data :name_en "") (:sku data))
                               (str/replace #"-" ","))
                           (:meta_keywords data))
          :meta_description (:meta_description data)
          :short_description (if (:description_en data) (utils/trunc (str (:description_en data)) 500))
          :additional_attributes (convert-addattr data)
          :configurable_variations (convert-configvar data list)
          :weight (if (:weight data) (:weight data) 0)
          :visibility (if (and (= (get data :product_type) "simple")
                               (parent-product (:sku data) list)
                               (not= (parent-product (:sku data) list) data))
                        "Not Visible Individually"
                        "Catalog, Search")
          :related_skus (genereate-related-skus data)
          :url_key (if (:url_key data)
                     (:url_key data)
                     (name->url-key (get data :name_en "") (:sku data)))}
         (convert-images data list)))

(defn simple-data [data _]
  (->> (merge {:name (:name_en data)
          :sku (:sku data)
          :price (:price data)
          :special_price (:special_price data)
          :product_type (:product_type data)
          :categories (and (:categories data)
                           (-> (:categories data)
                               (str/replace #",Categories" ";Categories")
                               (str/replace #", Categories" ";Categories")
                               (str/replace #", " " ")
                               (str/replace #"," " ")
                               (str/replace #";" ",")))
          :url_key (if (:url_key data)
                     (:url_key data)
                     (name->url-key (get data :name_en "") (:sku data)))
          :description (generate-description data :description_fr)
          :meta_title (:meta_title data)
          :meta_keywords (:meta_keywords data)
          :meta_description (:meta_description data)
          :short_description (if (:description_en data) (utils/trunc (str (:description_en data)) 500))
          :weight (if (:weight data) (:weight data))
              :related_skus (genereate-related-skus data)})
       (remove #(empty? (str/trim (str (second %)))))
      (into {})))



(defn fr-data [data _]
  (merge empty-product-info
         default-product-info
         {:name (:name_fr data)
          :sku (:sku data)
          :store_view_code "french"
          :product_type (:product_type data)
          :description (generate-description data :description_fr)
          :url_key (name->url-key (get data :name_fr "") (:sku data))}))

(defn es-data [data _]
  (merge empty-product-info
         default-product-info
         {:name (:name_es data)
          :sku (:sku data)
          :store_view_code "spanish"
          :product_type (:product_type data)
          :description (generate-description data :description_es)
          :url_key (name->url-key (get data :name_es "") (:sku data))}))

(defn de-data [data _]
  (merge empty-product-info
         default-product-info
         {:name (:name_de data)
          :sku (:sku data)
          :store_view_code "spanish"
          :product_type (:product_type data)
          :description (generate-description data :description_es)
          :url_key (name->url-key (get data :name_de "") (:sku data))}))


(defn- str->map [s item-sp val-sp]
  (if (empty? s)
    {}
    (reduce #(let [info (str/split %2 val-sp)]
               (assoc %1 (first info) (second info)))
            {}
            (str/split s item-sp))))

(defn find-duplicate-key
  "找出map-list中值重复的key"
  [map-list]
  (keys (filter
         #(< (count (second %)) 2)
         (apply merge-with
                into
                (map (partial walk/walk (fn [[k v]] {k #{v}}) identity) map-list)))))


(defn replace-attributes
  [data]
  (let [format-data (map #(update % :additional_attributes (fn [n] (str->map n #"," #"="))) data)
        dupkeys (find-duplicate-key (map :additional_attributes format-data))]
    (map #(-> %
              (assoc :configurable_variation_labels "")
              (update :additional_attributes
                      (fn [n] (-> (apply dissoc n dupkeys)
                                  (maps->string-format)))))
         format-data)))

(defn replace-variation [data]
  (if (and (= (:product_type (first data)) "configurable")
           (> (count data) 2))
    (let [subrs (-> (subvec (vec data) 1)
                    (->> (map (fn [item]
                                (if-not (empty? (:additional_attributes item))
                                  (str "sku=" (:sku item) "," (:additional_attributes item))
                                  (str "sku=" (:sku item)))))))]
      (cons (assoc (first data) :configurable_variations (str/join "|" subrs))
            (subvec (vec data) 1)))
    data))



(defn remove-duplicate-attribute
  [target-list]
  (let [group-data (group-by #(-> % :sku (str/split #"-") first) target-list)]
    (mapcat (comp replace-variation replace-attributes) (vals group-data))))




(defn create-magento-product [data]
  (merge empty-product-info default-product-info data))

(defn do-all-data-logic
  ([in-data out-file convert-data]
   (let [list (-> in-data (->> (map trim-map-val)))]
     (-> (map #(-> (convert-data % list) create-magento-product) list)
         (remove-duplicate-attribute)
         (utils/maps->csv-file out-file)))))

(defn do-simple-logic
  [in-data out-file convert-data]
  (let [list (-> in-data (->> (map trim-map-val)))]
    (-> (map #(convert-data % list) list)
        (utils/maps->csv-file out-file))))


;;;;;;;;;;;;;;;;;;;;;;;;;; attribute ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn import-attribute-options
  "生成属性值，会删除重建"
  ([dataset conn]
   (doseq [[k coll] dataset]
     (when-not (empty? (remove nil? coll))
       (let [attr-id (k attribute-map)
             dbconn (if (= conn "test") db/arikami-test-db db/arikami-db)
             exist-text (set (map :value (db/get-attribute-option-value attr-id dbconn)))
             diff-options (cset/difference (set (remove nil? coll)) exist-text)]
         (when-not (empty? diff-options)
           (db/insert-options attr-id diff-options dbconn))))))
  ([dataset]
   (import-attribute-options dataset "prod")))



;;;;;;;;;;;;;;;;;;;;;;;;;; verify ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn sub-coll [coll start end]
  (-> (into [] coll)
      (subvec start end)))

(defn get-media-path []
  (-> env :media-path))

(defn verify-files [paths]
  (let [exists-file #(.exists(io/as-file %))]
    (remove nil? (pmap #(if (exists-file (str (get-media-path) "/" %)) nil %) paths))))

(defn check-data [list]
  (let [data (map trim-map-val list)]
    (when-not (s/valid? ::data-list data)
      (-> (s/explain-data ::data-list data)
          (get :clojure.spec.alpha/problems)
          (->> (map #(get % :in)))))))


(defn verify-sku [list]
  (map first
       (filter #(> (count (second %)) 1) (group-by :sku list))))

(defn delIndex [conn]
  (if (= conn "prod")
    (db/delete-all-category-index db/arikami-db)
    (db/delete-all-category-index db/arikami-test-db)))

(defn reIndex [mode]
  (let [dir (if (= mode "test")
              (-> env :arikami-test-root)
              (-> env :arikami-root))]
    (shell/sh "php" "bin/magento" "indexer:reindex" :dir dir)))

(defn flush-cache [mode]
  (let [dir (if (= mode "test")
              (-> env :arikami-test-root)
              (-> env :arikami-root))]
    (shell/sh "php" "bin/magento" "cache:flush" :dir dir)))

;;;;;;;;;;;;;;;;;;;;;;;;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
#_(def excel-data (utils/read-excel->map "g:/listdata/3-simple.xlsx" "upload_template"))
#_(do-simple-logic excel-data "g:/555.csv" simple-data)

#_(def excel-data (utils/read-excel->map "g:/listdata/3-all.xlsx" "upload_template"))
#_(do-all-data-logic excel-data "g:/555.csv" convert-data)
#_(let [list excel-data]
  (-> (map #(-> (convert-data % list) create-magento-product) list)
      #_(remove-duplicate-attribute)
      ))


#_(println (check-data excel-data))


                                        ; 校验图片存在
#_(verify-files (set (mapcat #(str/split (:image %) #";") new-excel-data)))
                                        ; 校验SKU
#_(verify-sku excel-data)
#_(verify-sku new-excel-data)

                                        ; 导入属性值
#_(import-attribute-options (attribute-dataset excel-data) "test")

                                        ; 删除index(可选)
#_(db/delete-all-category-index)

                                        ; 生成主导入文件
#_(do-all-data-logic excel-data "G:/data1.csv" convert-data)
#_(do-all-data-logic excel-data "G:/data1-fr.csv" fr-data)
#_(do-all-data-logic excel-data "G:/data1-es.csv" es-data)


#_(do-all-data-logic new-excel-data "G:/data2.csv" convert-data)
#_(do-all-data-logic new-excel-data "G:/data2-fr.csv" fr-data)
#_(do-all-data-logic new-excel-data "G:/data2-es.csv" es-data)

