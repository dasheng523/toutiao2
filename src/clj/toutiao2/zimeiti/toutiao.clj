(ns toutiao2.zimeiti.toutiao
  (:require [clj-webdriver.taxi :refer :all]
            [cheshire.core :as json]
            [dk.ative.docjure.spreadsheet :as sheet]
            [toutiao2.utils :as utils]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [toutiao2.zimeiti.config :as config]
            [toutiao2.zimeiti.driver :as tdriver]
            [toutiao2.zimeiti.grap :as grap])
  (:import (java.text SimpleDateFormat)
           (org.openqa.selenium.interactions Actions)))


(def sleep-time 1000)
(def wait-time (* 3600 1000))
(def driver {})

(defn- read-excel [path cols]
  (->> path
       (sheet/load-workbook)
       (sheet/select-sheet "html")
       (sheet/select-columns cols)))


(defn recover-cookies [driver user]
  (let [mycookies (utils/deserialize (str (config/get-cookies-path) user ".cookies"))]
    (doseq [co mycookies]
      (add-cookie driver co))))

(defn save-cookies [driver user]
  (io/make-parents (str (config/get-cookies-path) user ".cookies"))
  (utils/serialize
   (str (config/get-cookies-path) user ".cookies")
   (map #(dissoc % :cookie) (cookies driver))))


(defn handle-reset-page [driver]
  (when (and
         (exists? driver "span.action-text")
         (str/includes? (html driver "span.action-text") "撤销"))
    (click driver "span.action-text")))

(defn enter-post-page [driver]
  (click driver "ul.garr-header-nav li:nth-child(2) a")
  (click driver "ul.tui-menu-container li:nth-child(3) a"))


(defn enter-post-page1 [driver]
  (click driver "div.shead_right div.shead-post")
  (Thread/sleep sleep-time)
  (when (exists? driver "div.dialog-footer .tui-btn-negative")
    (click driver "div.dialog-footer .tui-btn-negative")
    (Thread/sleep (* 2 sleep-time))
    (click driver "div.shead_right div.shead-post"))
  (Thread/sleep (* 2 sleep-time))
  (click driver "ul.pgc-title li:nth-child(3) a")
  (Thread/sleep sleep-time)
  (handle-reset-page driver)
  (wait-until driver #(exists? % "div.content-wrapper div.upload-bg") wait-time 1000))



(defn open-toutiao [driver user]
  (to driver "https://mp.toutiao.com/profile_v2/")
  (wait-until driver #(= (title %) "主页 - 头条号") wait-time 1000)
  (when (exists? driver "div.btn-wrap span.got-it")
    (click driver "div.btn-wrap span.got-it")))


(defn- handle-no-money [driver]
  (click driver "div#pgc-add-product div.pgc-dialog div.dialog-footer button:nth-child(1)")
  (click driver "div.gallery-footer button.confirm-btn")
  (Thread/sleep sleep-time)
  (click driver (str "div.pagelet-figure-gallery-item:last-child div.gallery-action i.icon-delete")))

(defn add-pic [driver {:keys [pic desc]}]
  (Thread/sleep (* 2 sleep-time))
  (when pic
    (if (exists? driver "div.upload-btn button.pgc-button")
      (click driver "div.upload-btn button.pgc-button")
      (click driver "div.figure-state button.figure-add-btn"))
    (send-keys driver "div.upload-handler input" pic)
    (wait-until driver
                #(str/includes? (html % "div.drag-tip") "完成")
                wait-time 1000)
    (click driver "div.button-group button.confirm-btn")
    (Thread/sleep sleep-time)
    (input-text driver (str "div.content-wrapper div.pagelet-figure-gallery-item:last-child div.gallery-txt textarea") desc)
    (wait-until driver
                #(not (exists? % "div.pgc-dialog"))
                wait-time 500)))

(defn add-item [driver {:keys [pic link title desc]}]
  (Thread/sleep sleep-time)
  (when pic
    (if (exists? driver "div.upload-btn button.pgc-button")
      (click driver "div.upload-btn button.pgc-button")
      (click driver "div.figure-state button.figure-add-btn"))
    (send-keys driver "div.upload-handler input" pic)
    (wait-until driver #(exists? % "div.button-group button.confirm-btn") wait-time 1000)
    (wait-until driver #(and (.startsWith (text % "div.image-footer div.drag-tip") "上传完成")
                             (exists? % "span.success-show"))
                wait-time 1000)
    (click driver "div.button-group button.confirm-btn")
    (Thread/sleep sleep-time)
    (input-text driver (str "div.content-wrapper div.pagelet-figure-gallery-item:last-child div.gallery-txt textarea") desc)
    (click driver (str "div.content-wrapper div.pagelet-figure-gallery-item:last-child div.gallery-sub-sale span.slink"))
    (input-text driver "input[name=product_url]" link)
    (Thread/sleep sleep-time)
    (click driver "span.product-info-fetch")
    (wait-until driver #(not= "" (value % "div.info-wrap div.product-info-item:nth-child(1) input")) wait-time 1000)
    (clear driver "div.info-wrap div.product-info-item:nth-child(1) input")
    (input-text driver "div.info-wrap div.product-info-item:nth-child(1) input" title)
    (input-text driver ".tui-input-wrapper textarea[name=recommend_reason]" desc)
    (Thread/sleep sleep-time)
    (click driver "div.gallery-footer button.confirm-btn")
    (wait-until driver
                #(not (exists? % "div.pgc-dialog"))
                wait-time 500)))

#_(def aa (grap/product-item-info "http://www.51taojinge.com/jinri/temai_content_article.php?id=1144085&check_id=2"))
#_(add-item @mydriver (first
                     (remove-duplicate
                      (filter #(not-empty (:link %)) (:goods aa))
                      :link)))

(defn- remove-duplicate [coll k]
  (map #(-> % second first)
       (group-by #(if (empty? (k %)) % (k %)) coll)))


(defn auto-fill-article [driver {:keys [atitle goods]}]
  (doseq [info (take 9 (remove-duplicate goods :link))]
    (println info)
    (if (empty? (:link info))
      (add-pic driver info)
      (add-item driver info)))
  (input-text driver "div.article-title-wrap input" atitle)
  (click driver "div.pgc-radio label.tui-radio-wrapper:nth-child(3) span.tui-radio-text"))

(defn- read-data-from-txt [path]
  (-> path
      slurp
      (str/split #"\n")
      (->> (map str/trim))
      (->> (remove #(= "" %)))))


(defn- wail-for-ready-post [driver]
  (wait-until driver
              #(str/includes? (html % "li.category-item.selected") "全部图文")
              wait-time
              1000)
  (Thread/sleep sleep-time))

(defonce mydriver (atom {}))

(defn open-chrome
  "打开头条"
  []
  (reset! mydriver (tdriver/create-chrome-driver))
  (to @mydriver "https://mp.toutiao.com/profile_v2/"))


(defn do-save-cookies [username]
  (save-cookies @mydriver username))


(defn do-recover-cookies [username]
  (recover-cookies @mydriver username)
  (to @mydriver "https://mp.toutiao.com/profile_v2/"))


(defn doautorun [urls]
  (let [flist (for [url urls]
                (future (doall (grap/product-item-info url))))]
    (doseq [fut flist]
        (enter-post-page @mydriver)
        (auto-fill-article @mydriver @fut)
        (wail-for-ready-post @mydriver))))




#_(doall (grap/product-item-info "http://www.51taojinge.com/jinri/temai_content_article.php?id=1144204&check_id=2"))

#_(doautorun ["http://www.51taojinge.com/jinri/temai_content_article.php?id=1145969&check_id=2"
            "http://www.51taojinge.com/jinri/temai_content_article.php?id=1144085&check_id=2"])



#_(grap/product-item-info "http://www.51taojinge.com/jinri/temai_content_article.php?id=1170322&check_id=2")


#_(defn run [user]
  (let [mydriver (tdriver/create-chrome-driver)
        links (read-data-from-txt config/url-data)
        flist (for [link links] (future (grap/product-item-info link)))]
    (do-recover-cookies mydriver user)
    (open-toutiao mydriver user)
    (enter-post-page mydriver)
    (dotimes [n (count links)]
      (let [link (nth links n)
            fut (nth flist n)]
        (auto-fill-article mydriver @fut)
        (wail-for-ready-post mydriver)
        (enter-post-page mydriver)))))










