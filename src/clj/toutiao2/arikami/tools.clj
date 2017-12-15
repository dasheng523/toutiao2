(ns toutiao2.arikami.tools
  (:require [toutiao2.utils :as utils]
            [clj-time.local :as l-t]
            [clojure.string :as str]))

(defn- name->url-key [name sku]
  (-> name
      (str "-" sku)
      (str/lower-case)
      (str/replace #" " "-")))


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
                  :weight                     (/ (get info :weight 0) 1000)
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
                  :visibility (if (= (get info :product_type "simple") "simple") "Search" "Catalog, Search")
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
  (let [img-str (if (string? (:des_image data))
                    (-> (str/split (:des_image data) #";")
                        (->> (map #(str "http://www.arikami.com/media/Products/" %)))
                        (->> (map #(str "<img src=\"" % "\">")))
                        (->> (str/join "\n"))))
        desc (if (> (count (:description_en data)) 0)
               (-> (:description_en data)
                   (str/split #"\n")
                   (->> (map #(str "<p>" % "</p>"))
                        (str/join "\n"))))]
    (str desc "\n" img-str)))


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

(defn sub-result [coll]
  (-> (into [] coll)
       (subvec 8 15)))

(let [list (-> (utils/read-excel
                 "g:/upload_template2.xlsx"
                 "upload_template"
                 excel-map)
               rest
               (->> (map remove-map-space)))]
  (-> (map #(convert-data % list) list)
      (sub-result)
      (create-magento-product-list)
      (utils/maps->csv-file "g:/2333.csv")))



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

