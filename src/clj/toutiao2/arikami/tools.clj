(ns toutiao2.arikami.tools
  (:require [toutiao2.utils :as utils]
            [clj-time.local :as l-t]
            [clojure.string :as str]))

(defn- name->url-key [name sku]
  (-> name
      (str/lower-case)
      (str/replace #" " "-")
      (str sku)))


(defn create-magento-product-list [list file]
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
                  :swatch_image               (:base_image info)
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
                  :meta_keywords              (get info :meta_keywords)
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
                  :weight                     (get info :weight "")
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
                  :visibility "Catalog, Search"
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
    (utils/maps->csv-file data file)))

#_(create-magento-product-list
  [{:name ":name"
    :price ":price"
    :base_image ":base_image"
    :additional_images ":additional_images"
    :sku ":sku"
    :categories ":categories"
    :product_type ":product_type"
    :configurable_variations ":configurable_variations"
    :meta_title ":meta_title"
    :meta_keywords ":meta_keywords"
    :meta_description ":meta_description"
    :short_description ":short_description"
    :description ":description"
    :additional_attributes ":additional_attributes"
    :weight ":weight"}
   {:description "",
    :configurable_variations nil,
    :meta_title nil,
    :name "",
    :meta_keywords nil,
    :short_description nil,
    :product_type "simple",
    :sku "XMMB2",
    :base_image "ConsumerElectronics/SmartEletronics/WearableDevices/XMMB2/SC-XMMB2-1.jpg",
    :additional_images "ConsumerElectronics/SmartEletronics/WearableDevices/XMMB2/SC-XMMB2-1.jpg;ConsumerElectronics/SmartEletronics/WearableDevices/XMMB2/SC-XMMB2-2.jpg;ConsumerElectronics/SmartEletronics/WearableDevices/XMMB2/SC-XMMB2-3.jpg",
    :categories "Default Category/Consumer Electronics,Default Category/Consumer Electronics/Smart Eletronics,Default Category/Consumer Electronics/Smart Eletronics/Wearable Devices",
    :weight 70.0,
    :meta_description nil,
    :additional_attributes "color=Black,material=Aluminum+Plastic,size=15.7*10.5*40.3mm",
    :price 32.99}]
  "g:/2224.csv")


(defn get-attrs [data]
  (-> {"color" (:color data)
       "material" (:material data)
       "size" (:size data)
       "type" (:type data)
       "capacity" (:capacity data)
       "option" (:option data)}
      (->> (filter second)
           (into {}))))

(defn maps->string-format [m]
  (->> m
       (map #(str (-> % first (str/trim)) "=" (-> % second (str/trim))))
       (str/join ",")))

(defn convert-addattr [data]
  (-> (get-attrs data)
      (maps->string-format)))

(defn generate-description [data]
  (let [img-str (-> (str/split (:des_image data) #";")
                    (->> (map #(str "http://www.arikami.com/media/Products/" %)))
                    (->> (map #(str "<img src=\"" % "\">")))
                    (->> (str/join "\n")))]
    (if (> (count (:description_en data)) 0)
      (str (:description_en data) "\n" img-str)
      img-str)))

(defn convert-configvar [data list]
  (if (= (:product_type data) "configurable")
    (let [sku (:sku data)
          flist (filter #(and (str/starts-with? (:sku %) sku) (not= sku (:sku %))) list)
          config-map (map #(merge (get-attrs %) {"sku" (:sku %)}) flist)]
      (->> config-map
           (map maps->string-format)
           (str/join "|")))))

(defn convert-data [data list]
  {:name (if (:name_en data) (:name_en data) (:sku data))
   :price (:price data)
   :base_image (-> (:image data)
                   (str/split #";")
                   first)
   :additional_images (str/replace (:image data) #";" ",")
   :sku (:sku data)
   :categories (:categories data)
   :product_type (:product_type data)
   :configurable_variations (convert-configvar data list)
   :meta_title (:meta_title data)
   :meta_keywords (:meta_keywords data)
   :meta_description (:meta_description data)
   :short_description (if (:description_en data) (utils/trunc (:description_en data) 500))
   :description (generate-description data)
   :additional_attributes (convert-addattr data)
   :weight (:weight data)})

(def excel-map
  {:A :sku
   :B :product_type
   :C :categories
   :D :name_en
   :E :name_fr
   :F :name_es
   :G :description_en
   :H :description_fr
   :I :description_es
   :J :weight
   :K :color
   :L :material
   :M :size
   :N :type
   :O :capacity
   :P :option
   :Q :price
   :R :special_price
   :S :special_price_from_date
   :T :special_price_to_date
   :U :meta_title
   :V :meta_keywords
   :W :meta_description
   :X :image
   :Y :des_image
   :Z :swatch_image
   :AA :new_from_date
   :AB :new_to_date
   :AC :qty
   :AD :related_skus
   :AE :crosssell_skus
   :AF :upsell_skus
   :AG :associated_skus})

(defn- remove-map-space [m]
  (if (map? m)
    (into {}
          (map (fn [one]
                 (if (string? (second one))
                   {(first one) (str/trim (second one))}
                   one)) m))))


(let [list (-> (utils/read-excel
                 "g:/upload_template.xlsx"
                 "upload_template"
                 excel-map)
               rest
               (->> (map remove-map-space)))]
  (-> (map #(convert-data % list) list)
      (create-magento-product-list "g:/2333.csv")))

;; des_image描述图片放在描述的哪个位置？
