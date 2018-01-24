(ns toutiao2.arikami.tools
  (:require [toutiao2.utils :as utils]
            [clj-time.local :as l-t]
            [clojure.string :as str]
            [clj-http.client :as http]
            [clojure.spec.alpha :as s]
            [clojure.walk :as walk]))

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
   :tax_class_name             "Taxable Goods"
   :is_qty_decimal             0
   :out_of_stock_qty           0.0000
   :product_websites           "base"
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



#_(defn create-magento-product-list [list]
  (let [data (map
               (fn [info]
                 {:use_config_max_sale_qty    1
                  :description                (get info :description "")
                  :bundle_values              ""
                  :thumbnail_image            (get info :base_image "")
                  :bundle_sku_type            ""
                  :custom_design_from         ""
                  :manage_stock               1
                  :custom_design_to           ""
                  :small_image_label          ""
                  :use_config_enable_qty_inc  0
                  :associated_skus            ""
                  :map_enabled                ""
                  :swatch_image               ""
                  :bundle_shipment_type       ""
                  :enable_qty_increments      0
                  :new_from_date              ""
                  :crosssell_skus             ""
                  :special_price              (get info :special_price "")
                  :configurable_variations    (get info :configurable_variations "")
                  :website_id                 0
                  :meta_title                 (get info :meta_title "")
                  :name                       (get info :name "")
                  :use_config_min_qty         1
                  :gift_message_available     ""
                  :min_cart_qty               1.0000
                  :related_skus               ""
                  :country_of_manufacture     ""
                  :use_config_backorders      1
                  :bundle_price_type          ""
                  :custom_design              ""
                  :product_online             1
                  :qty_increments             0.0000
                  :swatch_image_label         ""
                  :notify_on_stock_below      1
                  :display_product_options_in "Block after Info Column"
                  :upsell_position            ""
                  :meta_keywords              (-> (name->url-key (get info :name "") (get info :sku ""))
                                                  (str/replace #"-" ","))
                  :short_description          (get info :short_description)
                  :msrp_price                 ""
                  :product_options_container  ""
                  :use_config_manage_stock    1
                  :product_type               (get info :product_type "simple")
                  :hide_from_product_page     ""
                  :sku                        (get info :sku)
                  :related_position           ""
                  :updated_at                 (l-t/local-now)
                  :base_image                 (:base_image info)
                  :additional_images          (:additional_images info)
                  :tax_class_name             "Taxable Goods"
                  :is_qty_decimal             0
                  :out_of_stock_qty           0.0000
                  :bundle_weight_type         ""
                  :categories                 (:categories info)
                  :product_websites           "base"
                  :special_price_to_date      nil
                  :weight                     (* (get info :weight 0) 1000)
                  :max_cart_qty               1000
                  :use_config_qty_increments  1
                  :additional_image_labels    (get info :additional_image_labels nil)
                  :meta_description (get info :meta_description)
                  :map_price ""
                  :crosssell_position ""
                  :additional_attributes (get info :additional_attributes)
                  :qty 1000
                  :is_decimal_divided 0
                  :use_config_min_sale_qty 1
                  :is_in_stock 1
                  :use_config_notify_stock_qty 1
                  :url_key (name->url-key (get info :name "") (get info :sku ""))
                  :custom_layout_update ""
                  :bundle_price_view ""
                  :thumbnail_image_label ""
                  :special_price_from_date ""
                  :allow_backorders 0
                  :small_image (:base_image info)
                  :price (:price info)
                  :new_to_date ""
                  :visibility (if (and (= (get info :product_type) "simple")
                                       (parent-product (:sku info) list)
                                       (not= (parent-product (:sku info) list) info))
                                "Not Visible Individually"
                                "Catalog, Search")
                  :attribute_set_code "Default"
                  :configurable_variation_labels ""
                  :store_view_code (get info :store_view_code "")
                  :msrp_display_actual_price_type ""
                  :custom_options ""
                  :upsell_skus ""
                  :created_at (l-t/local-now)
                  :base_image_label ""
                  :page_layout ""})
               list)]
    data))



(defn- name->url-key [name sku]
  (-> name
      (str "-" sku)
      (str/lower-case)
      (str/replace #" " "-")))

(defn- sub-products [sku list]
  (filter #(and (str/starts-with? (:sku %) sku) (not= sku (:sku %))) list))

(defn- parent-product [sku list]
  (-> (filter #(= (:sku %) (-> sku (str/split #"-") first)) list)
      (first)))


(defn- format-attr [s]
  (if s
    (-> (if (number? s) (str (int s)) s)
        (str/replace #"," " -"))))

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

(defn generate-description [data]
  (let [img-str (if (string? (:des_image data))
                    (-> (str/split (:des_image data) #";")
                        (->> (map #(str "http://www.arikami.com/media/Products/" %)))
                        (->> (map #(str "<img src=\"" % "\">")))
                        (->> (str/join "\n"))))
        desc (if (string? (:description_en data))
               (-> (:description_en data)
                   (str/split #"\n")
                   (->> (map #(str "<p>" % "</p>"))
                        (str/join "\n"))))]
    (str desc "\n" img-str)))


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


(defn- attribute-set
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


(defn- remove-map-space [m]
  (into {}
        (map (fn [one]
               (if (string? (second one))
                 {(first one) (str/trim (second one))}
                 one)) m)))



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
  (let [image (-> (:image data)
                  (str/split #";")
                  first
                  (str/replace #"&" ""))]
    {:name (:name_en data)
     :price (:price data)
     :special_price (:special_price data)
     :base_image image
     :thumbnail_image image
     :small_image image
     :additional_images (convert-additional_images data list)
     :sku (:sku data)
     :categories (-> (:categories data) (str/replace #"," "") (str/replace #";" ","))
     :product_type (:product_type data)
     :configurable_variations (convert-configvar data list)
     :meta_title (:meta_title data)
     :meta_keywords (-> (name->url-key (get data :name_en "") (get data :sku ""))
                        (str/replace #"-" ","))
     :meta_description (:meta_description data)
     :short_description (if (:description_en data) (utils/trunc (:description_en data) 500))
     :description (generate-description data)
     :additional_attributes (convert-addattr data)
     :weight (:weight data)
     :visibility (if (and (= (get data :product_type) "simple")
                          (parent-product (:sku data) list)
                          (not= (parent-product (:sku data) list) data))
                   "Not Visible Individually"
                   "Catalog, Search")
     :url_key (name->url-key (get data :name_en "") (get data :sku ""))}))


(defn create-magento-product-list [data]
  (merge empty-product-info default-product-info data))



(defn do-write []
  (let [list (-> (utils/read-excel->map "/Users/huangyesheng/Documents/upload_template-1228-v3.xls" "upload_template")
                 (->> (map remove-map-space)))]
    (if (s/valid? ::data-list list)
      (-> (map #(-> (convert-data % list) create-magento-product-list) list)
          (create-magento-product-list)
          (utils/maps->csv-file "/Users/huangyesheng/Documents/2333.csv"))
      (-> (s/explain-data ::data-list list)
          (get :clojure.spec.alpha/problems)))))

(do-write)

;;;;;;;;;;;;;;;;;;;;;;;;;; verify-images ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sub-coll [coll start end]
  (-> (into [] coll)
      (subvec start end)))

(defn verify-images
  "验证图片有效性"
  []
  (let [data (-> (utils/read-excel->map "/Users/huangyesheng/Documents/upload_template-1228-v3.xls" "upload_template")
                 (->> (map remove-map-space)))
        urls (->> (mapcat (fn [info]
                            (-> (str (:image info) ";" (:des_image info))
                                (str/split #";")
                                (->> (map #(str "http://www.arikami.com/media/Products/" (str/trim %))))))
                          data)
                  (into #{}))]
    (println (count urls))
    (future (doseq [url (sub-coll urls 1 500)]
              (try
                (if (not= (:status (http/head url)) 200)
                  (println url))
                (catch Exception e
                  (println url)))))
    (future (doseq [url (sub-coll urls 500 1000)]
              (try
                (if (not= (:status (http/head url)) 200)
                  (println url))
                (catch Exception e
                  (println url)))))
    (future (doseq [url (sub-coll urls 1000 1500)]
              (try
                (if (not= (:status (http/head url)) 200)
                  (println url))
                (catch Exception e
                  (println url)))))
    (future (doseq [url (sub-coll urls 1500 1776)]
              (try
                (if (not= (:status (http/head url)) 200)
                  (println url))
                (catch Exception e
                  (println url)))))))

(verify-images)

;;;;;;;;;;;;;;;;;;;;;;;;;; Fr ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn find-sku [sku english-data]
  (-> english-data
      (->> (filter #(= sku (:sku %))))
      (first)))

(def english-data (utils/csv-file->maps "g:/catalog_product_20180115_015420.csv"))

(defn- generate-fr-description [data]
  (let [resource-info (find-sku (:sku data) english-data)]
    (when resource-info
      (if (= "" (-> (:description_fr data) str str/trim))
        ""
        (-> resource-info
            :description
            (str/split #"<p>" 2)
            first
            (str "\n" (:description_fr data)))))))


(defn fr-data [data]
  (merge empty-product-info
         {:name (:name_fr data)
          :sku (:sku data)
          :store_view_code "french"
          :attribute_set_code "default"
          :product_type "configurable"
          :description (generate-fr-description data)
          :url_key (name->url-key (get data :name_fr "") (get data :sku ""))}))


(defn map-val-trim [m]
  (reduce #(update %1 (first %2) (comp str/trim str)) m m))

(defn do-fr []
  (-> (utils/read-excel->map "g:/FR.xlsx" "upload_template")
      (->> (filter #(and (not= (:sku %) "")
                         (not (str/includes? (:sku %) "-")))))
      (->> (map (comp fr-data map-val-trim)))
      (utils/maps->csv-file "g:/fr.csv")))

(do-fr)



;;;;;;;;;;;;;;;;;;;;;;;;;; 修改属性 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def source-data (utils/csv-file->maps "g:/catalog_product_20180123_074249.csv"))

(defn- str->map [s item-sp val-sp]
  (if (empty? s)
    {}
    (reduce #(let [info (str/split %2 val-sp)]
               (assoc %1 (first info) (second info)))
            {}
            (str/split s item-sp))))


(def group-data (group-by #(-> % :sku (str/split #"-") first) source-data))

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
    (let [subrs (-> (subvec (vec data) 2)
                    (->> (map (fn [item]
                                (if-not (empty? (:additional_attributes item))
                                  (str "sku=" (:sku item) "," (:additional_attributes item))
                                  (str "sku=" (:sku item)))))))]
      (cons (assoc (first data) :configurable_variations (str/join "|" subrs))
            (subvec (vec data) 1)))
    data))

(defn modify-description [s]
  (if (re-find #"</p>" s)
    (let [html (str/split s #"</p>\n<img")
          html-vec (-> html
                       (first)
                       (str/split #"\n"))
          total (count html-vec)]
      (-> (conj (sub-coll html-vec 0 (/ total 27))
                (str (last html-vec) "</p>"))
          (->> (str/join #""))
          (#(if (second html) (str % "<img" (second html)) %))))
    s))

(defn do-modify-attr []
  (-> (mapcat (comp replace-variation replace-attributes) (vals group-data))
      (->> (map #(update % :weight (fn [n] (if-not (empty? n) (float (/ (utils/parse-int n) 1000)))))))
      (->> (map #(update % :description modify-description)))
      (utils/maps->csv-file "g:/remove-attr.csv")))

(do-modify-attr)


