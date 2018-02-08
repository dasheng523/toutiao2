(ns toutiao2.ave40.manager
  (:require [toutiao2.ave40.db :refer :all]
            [clojure.tools.logging :as log]
            [clojure.string :as string]))

(defn copy-source-article [table]
  (let [list (select-all article-db {:table table})]
    (doseq [info list]
      (data-insert! "source_article" {"url" (:url info) "html" (:html info)}))))

#_(let [list (select-all article-db {:table "article2" :cols ["title" "spinner_title" "spinner_article"]})]
  (doseq [info list]
    (update-data article-db {:table "articles"
                             :updates {:spinner_title (:spinner_title info)
                                       :spinner_article (:spinner_article info)}
                             :where (str "title='" (:title info) "'")})))

