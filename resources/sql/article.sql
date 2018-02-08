-- :name create-source_article-table :!
-- :doc Create source_article table
CREATE TABLE `source_article` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `url` varchar(255) NOT NULL,
  `html` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8


-- :name drop-source_article-table :!
-- :doc Drop source_article table if exists
drop table if exists `source_article`

-- :name insert-table-data :i!
insert into :i:table
(:i*:cols)
values
(:v*:vals)


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


-- :name insert-table-tuple :! :n
-- :doc Insert multiple characters with :tuple* parameter type
insert into :i:table
(:i*:cols)
values :tuple*:datas

-- :name delete-table-data-by-url :i!
delete from :i:table
where url in (:v*:urls)

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

-- :name select-one :? :1
-- :doc select one record
select
--~ (if (seq (:cols params)) ":i*:cols" "*")
from :i:table
/*~
(if (:where params)
  (str "where " (:where params)))
~*/

-- :name get-by-url :? :1
-- :doc get-by-url
select *
from :i:table
where `url`=:url

-- :name get-by-id :? :1
-- :doc get-by-id
select *
from :i:table
where `id`=:id

-- :name select-article-by-url :? :1
-- :doc select source_article by url
select *
from source_article
where `url` = :url

-- :name select-needfetch-by-url :? :1
-- :doc select source_article by url
select *
from need_fetch
where `url` = :url


-- :name pop-unvisit-url :!
delete from need_fetch
where `url` = :url

-- :name get-one-need_fetch :? :1
-- :doc get one limit amount record
select *
from need_fetch
limit 1

-- :name get-articel-by-id :? :1
-- :doc get-articel-by-id
select *
from source_article
where `id`=:id


-- :name get-all-needfetch :? :*
-- :doc get all needfetch data
select *
from need_fetch

-- :name get-all-urlhtml :? :*
-- :doc get all article data
select *
from source_article

-- :name get-all-article-html :? :*
-- :doc get all article html
select *
from source_article
WHERE html like '%entry-header overlay%';

-- :name get-article-by-url :? :1
-- :doc get article by url
select *
from article
where `url` = :url


-- :name select-article2-limit-10
-- :doc xxxx
select *
from article2
limit 2


-- :name select-article2-by-spinner-null
-- :doc get article by spinner-null
select *
from article2
where ISNULL(spinner_article)

-- :name select-rand-image :? :*
-- :doc select-rand-image
SELECT * FROM image
where `tag` = :tag and not ISNULL(url)
ORDER BY RAND() LIMIT 3;