(ns toutiao2.zawu.niushida
  (:require [toutiao2.driver :as tdriver]
            [toutiao2.config :as config]
            [etaoin.api :refer :all]
            [etaoin.keys :as ek]
            [clojure.core.async :as async]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [toutiao2.config :refer [isWindows?]]))


(def content-chan (async/chan))

(defn handle-content [content]
  (println content)
  (async/put! content-chan content))

(defn- search-driver []
  (chrome
   {:path-driver (.getPath (io/resource (tdriver/get-chromedriver-path)))
    :capabilities {}})
  #_(firefox
   {:path-driver (.getPath (io/resource (tdriver/get-firefox-path)))
    :capabilities {}}))

(defn is-has-texts [source ks]
  (if (empty? ks)
    false
    (if (str/includes? source (first ks))
      true
      (recur source (rest ks)))))

(defn souhu-content [driver]
  (get-element-text driver {:css ".article"}))

(defn other-content [driver]
  (get-source driver))

(defn dispatch-content-fn [url]
  (condp #(str/includes? %2 %1) url
    "sohu.com" souhu-content
    other-content))


(defn souhu-search [driver searchkey]
  (go driver (str "http://search.sohu.com/?keyword=" searchkey))
  (loop [i 1]
    (let [detail-node [{:tag :div :class "ArticleFeed"}
                       {:tag :div :index i}
                       {:tag :div :class "ant-card-body"}
                       {:tag :a}]]
      (when (and (exists? driver detail-node)
                 (< i 20))
        (scroll-query driver detail-node)
        (click driver detail-node)
        (wait 1)
        (switch-window driver (last (get-window-handles driver)))
        (wait 5)
        (handle-content (souhu-content driver))
        (close-window driver)
        (switch-window driver (first (get-window-handles driver)))
        (recur (+ i 1))))))


(defn baidu-search [driver searchkey]
  (go driver "https://www.baidu.com")
  (fill driver {:css "input.s_ipt"} searchkey ek/enter))

(defn baidu-current-page [driver index]
  (let [node [{:tag :div :class "result c-container " :index index}
              {:tag :h3}
              {:tag :a}]]
    (when (exists? driver node)
      (scroll-query driver node)
      (click driver node)
      (wait 1)
      (switch-window driver (last (get-window-handles driver)))
      (wait 5)
      (handle-content ((dispatch-content-fn (get-url driver)) driver))
      (close-window driver)
      (switch-window driver (first (get-window-handles driver)))
      (recur driver (+ index 1)))))



(defn save-cookies [driver user domain]
  (let [fcookie (str (config/get-cookies-path) "/" user "-" domain ".cookies")]
    (io/make-parents fcookie)
    (-> (get-cookies driver)
        (json/generate-string)
        (->> (spit fcookie)))))

(defn recover-cookies [driver user domain]
  (let [fcookies (-> (str (config/get-cookies-path) "/" user "-" domain ".cookies")
                    (slurp)
                    (json/parse-string true))]
    (doseq [coo fcookies]
      (set-cookie driver coo))))

(defn login-weibo [username password]
  (fill-human driver {:css "input#loginname"} username)
  (fill-human driver {:tag :input :name "password"} password)
  (click driver {:css "div.login_btn a"}))

(defn weibo-search-current-page
  ([driver index]
   (let [node [{:tag :div :class "WB_cardwrap S_bg2 clearfix" :index index}]
         readmore (conj node {:tag :a :class "WB_text_opt"})
         comment (into node [{:tag :ul :class "feed_action_info feed_action_row4"}
                             {:tag :li :index 3}
                             {:tag :em}])]
     (when (and (exists? driver node)
                (< index 100))
       (scroll-query driver node)
       (when (exists? driver readmore)
         (click driver readmore)
         (wait 1))
       (when (exists? driver comment)
         (click driver comment)
         (wait 2))
       (handle-content (get-element-text driver node))
       (recur driver (+ index 1)))))
  ([driver]
   (weibo-search-current-page driver 1)))

(defn search-weibo [driver kword]
  (fill-human driver {:css "input.W_input"} kword)
  (fill driver {:css "input.W_input"} ek/enter)
  (wait 3)
  (weibo-search-current-page driver))


(defn zhihu-search-current-page
  ([driver index]
   (let [node [{:tag :div :class "List-item" :index index}]
         readmore (conj node {:tag :button :class "Button ContentItem-more Button--plain"})
         comment (into node [{:tag :div :class "ContentItem-actions"}
                             {:tag :button}])]
     (when (and (exists? driver node)
                (< index 10))
       (scroll-query driver node)
       (when (exists? driver readmore)
         (click driver readmore)
         (wait 1))
       (when (exists? driver comment)
         (click driver comment)
         (wait 2))
       (handle-content (get-element-text driver node))
       (recur driver (+ index 1)))))
  ([driver]
   (zhihu-search-current-page driver 1)))

(let [dd (into [{:tag :div :class "List-item" :index 1}] [{:tag :div :class "ContentItem-actions"}
                                                          {:tag :button}])]
  (click driver dd))



(zhihu-search-current-page driver)

(def driver (search-driver))
(go driver "https://zhihu.com/")

(fill-human driver {:css ".SearchBar-input .Input"} "奔驰")
(fill driver {:css ".SearchBar-input .Input"} ek/enter)

#_(recover-cookies driver "yesheng" "zhihu")

#_(search-weibo driver "美女")

#_(souhu-search driver "纽仕达")
#_(baidu-search driver "奔驰")

#_(baidu-current-page 1)

#_(go driver (str "http://search.sohu.com/?keyword=" "纽仕达"))

#_(for [i (range 20)]
  (let [detail-node {:css (str ".ImageNewsCardContent > a:nth-child(" i ")")}]
    (exists? driver detail-node)))
#_(exists? driver {:css ".ImageNewsCardContent:nth-child(3) > a"})





