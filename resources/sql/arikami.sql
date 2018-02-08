-- :name insert-table-data :i!
insert into :i:table
(:i*:cols)
values
(:v*:vals)

-- :name insert-table-tuple :! :n
-- :doc Insert multiple characters with :tuple* parameter type
insert into :i:table
(:i*:cols)
values :tuple*:datas

-- :name update-data :i! :n
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
update :i:table set
/*~
(string/join ","
  (for [[field _] (:updates params)]
    (str (identifier-param-quote (name field) options)
        " = :v:updates." (name field))))
~*/
/*~
(if (:where params)
  (str "where " (:where params)))
~*/


-- :name select-all :? :*
-- :doc select all data from given table
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
select
--~ (if (seq (:cols params)) ":i*:cols" "*")
from :i:table
/*~
(if (:where params)
  (str "where " (:where params)))
~*/

-- :name select-first :? :1
-- :doc select first record
select
--~ (if (seq (:cols params)) ":i*:cols" "*")
from :i:table
/*~
(if (:where params)
  (str "where " (:where params)))
~*/



-- :name insert-attribute-option :i!
insert into `eav_attribute_option`
(`attribute_id`, `sort_order`)
values (:attribute_id, :sort_order)

-- :name insert-attribute-option-tuple :i!
insert into `eav_attribute_option`
(`attribute_id`, `sort_order`)
values :tuple*:datas

-- :name insert-attribute-option-value :i!
insert into `eav_attribute_option_value`
(`option_id`, `store_id`, `value`)
values (:option_id, :store_id, :value)

-- :name insert-attribute-option-value-tuple :i!
insert into `eav_attribute_option_value`
(`option_id`, `store_id`, `value`)
values :tuple*:datas

-- :name insert-attribute-option-swatch :i!
insert into `eav_attribute_option_swatch`
(`option_id`, `store_id`, `type`, `value`)
values (:option_id, :store_id, :type, :value)

-- :name insert-attribute-option-swatch-tuple :! :n
insert into `eav_attribute_option_swatch`
(`option_id`, `store_id`, `type`, `value`)
values :tuple*:datas

-- :name delete-attribute-option-by-attrs :i!
delete from `eav_attribute_option`
where attribute_id=:attrid

-- :name delete-attribute-option-value-by-options :i!
delete from `eav_attribute_option_value`
where option_id in (:v*:options)

-- :name delete-attribute-option-swatch-by-options :i!
delete from `eav_attribute_option_swatch`
where option_id in (:v*:options)

-- :name get-attribute-option-by-attrid :? :*
SELECT * FROM `eav_attribute_option` where attribute_id=:attrid

-- :name get-attribute-option-value-by-attrid :? :*
select * from `eav_attribute_option_value`
where `option_id` in (
    SELECT `option_id` FROM `eav_attribute_option` where attribute_id=:attrid
)


-- :name get-all-store-data :? :*
SELECT * FROM `store`

-- :name delete-all-category-index-data :i!
DELETE FROM `url_rewrite` where `entity_type`='category' or `entity_type`='product'


-- :name get-by-url :? :1
-- :doc get-by-url
select *
from :i:table
where `url`=:url


-- :name delete-table-data-by-url :i!
delete from :i:table
where url in (:v*:urls)