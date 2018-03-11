-- :name insert-data! :i!
insert into :i:table
(:i*:cols)
values
(:v*:vals)

-- :name insert-tuple-data! :! :n
insert into :i:table
(:i*:cols)
values :tuple*:datas

-- :name update-data! :i! :n
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

-- :name delete-data! :i! :n
delete from :i:table
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
/*~
(if (:orderBy params)
  (str "order by " (:orderBy params)))
~*/

-- :name select-first :? :1
-- :doc select first record
select
--~ (if (seq (:cols params)) ":i*:cols" "*")
from :i:table
/*~ (if (:where params) */
where :sql:where
/*~*/
/*~ (if (:orderBy params) */
order by :sql:orderBy
/*~*/
