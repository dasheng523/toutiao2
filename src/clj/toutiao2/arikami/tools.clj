(ns toutiao2.arikami.tools
  (:require [toutiao2.utils :as utils]
            [clj-time.local :as l-t]
            [clojure.string :as str]
            [clj-http.client :as http]
            [clojure.spec.alpha :as s]))

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

(defn create-magento-product-list [list]
  (let [data (map
               (fn [info]
                 {:use_config_max_sale_qty    1
                  :description                (:description info)
                  :bundle_values              ""
                  :thumbnail_image            (:base_image info)
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
                  :special_price              (:special_price info)
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
                  :weight                     (get info :weight 0)
                  :max_cart_qty               1000
                  :use_config_qty_increments  1
                  :additional_image_labels    (get info :additional_image_labels nil)
                  :meta_description (get info :meta_description)
                  :map_price ""
                  :crosssell_position ""
                  :qty 1000
                  :additional_attributes (get info :additional_attributes)
                  :is_decimal_divided 0
                  :url_key (name->url-key (get info :name "") (get info :sku ""))
                  :custom_layout_update ""
                  :bundle_price_view ""
                  :thumbnail_image_label ""
                  :special_price_from_date ""
                  :allow_backorders 0
                  :use_config_min_sale_qty 1
                  :small_image (:base_image info)
                  :is_in_stock 1
                  :use_config_notify_stock_qty 1
                  :price (:price info)
                  :new_to_date ""
                  :visibility (if (and (= (get info :product_type) "simple")
                                       (parent-product (:sku info) list)
                                       (not= (parent-product (:sku info) list) info))
                                "Not Visible Individually"
                                "Catalog, Search")
                  :attribute_set_code "Default"
                  :configurable_variation_labels ""
                  :store_view_code ""
                  :msrp_display_actual_price_type ""
                  :custom_options ""
                  :upsell_skus ""
                  :created_at (l-t/local-now)
                  :base_image_label ""
                  :page_layout ""})
               list)]
    data))

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

(defn sub-coll [coll start end]
  (-> (into [] coll)
      (subvec start end)))


(defn find-all-show-products
  []
  (-> (utils/read-excel->map "g:/upload_template3.xlsx" "upload_template")
      (->> (map remove-map-space)
           #_(filter #(= "configurable" (:product_type %)))
           (filter #(or (str/starts-with? (:image %) "ConsumerElectronics")
                        (str/starts-with? (:image %) "AnimeComicGame")))
           (group-by #(:sku_no %))
           (map first))))


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
  {:name (:name_en data)
   :price (:price data)
   :special_price (:special_price data)
   :base_image (-> (:image data)
                   (str/split #";")
                   first
                   (str/replace #"&" ""))
   :additional_images (convert-additional_images data list)
   :sku (:sku data)
   :categories (-> (:categories data) (str/replace #"," "") (str/replace #";" ","))
   :product_type (:product_type data)
   :configurable_variations (convert-configvar data list)
   :meta_title (:meta_title data)
   :meta_keywords (:meta_keywords data)
   :meta_description (:meta_description data)
   :short_description (if (:description_en data) (utils/trunc (:description_en data) 500))
   :description (generate-description data)
   :additional_attributes (convert-addattr data)
   :weight (:weight data)})


(defn verify-images [data]
  (let [urls (->> (mapcat (fn [info]
                           (-> (str (:image info) ";" (:des_image info))
                               (str/split #";")
                               (->> (map #(str "http://www.arikami.com/media/Products/" (str/trim %))))))
                         data)
                 (into #{}))]
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
    (future (doseq [url (sub-coll urls 1500 1780)]
              (try
                (if (not= (:status (http/head url)) 200)
                  (println url))
                (catch Exception e
                  (println url)))))))


#_(-> (utils/read-excel->map "g:/upload_template-1228-v2.xlsx" "upload_template")
    (->> (map remove-map-space))
    (verify-images))

(defn do-write []
  (let [list (-> (utils/read-excel->map "g:/upload_template-1228-v2.xlsx" "upload_template")
                 (->> (map remove-map-space)))]
    (if (s/valid? ::data-list list)
      (-> (map #(convert-data % list) list)
          (->>  (filter #(or (str/starts-with? (:base_image %) "ConsumerElectronics")
                             (str/starts-with? (:base_image %) "AnimeComicGame"))))
          (create-magento-product-list)
          (utils/maps->csv-file "g:/2333.csv"))
      (-> (s/explain-data ::data-list list)
          (get :clojure.spec.alpha/problems)))))

(do-write)




(-> (utils/read-excel->map "g:/upload_template-1228-v2.xlsx" "upload_template")
    (->> (map remove-map-space))
    (attribute-set))

#_(let [list (-> (utils/read-excel
                   "g:/upload_template2.xlsx"
                   "upload_template"
                   excel-map)
                 rest
                 (->> (map remove-map-space)))]
  (-> (map #(convert-data % list) list)
      (sub-result)
      (create-magento-product-list)
      (->> (map #(:url_key %))
           (group-by identity))))
