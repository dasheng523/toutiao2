(ns toutiao2.zawu.niushida
  (:require [toutiao2.driver :as tdriver]
            [toutiao2.config :as config]
            [etaoin.api :refer :all]
            [etaoin.keys :as ek]
            [clojure.core.async :as async]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [toutiao2.config :refer [isWindows?]]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [throw+ try+]]))

(defn- badwords []
  (let [path (if (isWindows?)
               (-> config/env :win-dired-path)
               (-> config/env :mac-dired-path))]
    (-> (slurp (str path "/badwords.txt"))
        (str/replace #"\r" "")
        (str/split #"\n")
        (->> (remove nil?)))))

(defn- has-badwords? [s]
  (some #(str/includes? s %)
        (badwords)))


(def content-chan (async/chan))

(defn handle-content [content]
  (when (has-badwords? content)
    (println content)
    (async/put! content-chan content)))

(has-badwords? "骗")
(badwords)

(defn- search-driver []
  (chrome
   {:path-driver (.getPath (io/resource (tdriver/get-chromedriver-path)))
    :args []
    :args-driver ["--ipc-connection-timeout=1"]
    :size [1920 800]})
  #_(firefox
   {:path-driver (.getPath (io/resource (tdriver/get-firefox-path)))
    :capabilities {}}))

(defn set-driver-timeout [driver]
  (with-resp driver :post
    [:session (:session @driver) :timeouts]
    {:type "page load" :ms 5000}
    _))

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


(defn baidu-current-page
  ([driver index]
   (let [node [{:tag :div :class "result c-container " :index index}
               {:tag :h3}
               {:tag :a}]]
     (when (exists? driver node)
       #_(scroll-query driver node)
       (click driver node)
       (wait 1)
       (switch-window driver (last (get-window-handles driver)))
       (wait 5)
       (handle-content ((dispatch-content-fn (get-url driver)) driver))
       (close-window driver)
       (switch-window driver (first (get-window-handles driver)))
       (recur driver (+ index 1)))))
  ([driver]
   (baidu-current-page driver 1)))

(defn baidu-search [driver searchkey]
  (go driver "https://www.baidu.com")
  (fill driver {:css "input.s_ipt"} searchkey ek/enter)
  (wait 2)
  (dotimes [n 5]
    (baidu-current-page driver)
    (when (exists? driver {:css "div#page > a:last-child"})
      (click driver {:css "div#page > a:last-child"})
      (wait 2))))


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

(defn login-weibo [driver username password]
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

(defn weibo-search [driver kword]
  (fill-human driver {:css "input.W_input"} kword)
  (fill driver {:css "input.W_input"} ek/enter)
  (wait 3)
  (weibo-search-current-page driver))


(defn zhihu-search-current-page
  ([driver index]
   (let [node [{:tag :div :class "List-item" :index index}]
         readmore (into node [{:css ".RichContent .ContentItem-more"}])
         comment (into node [{:css ".ContentItem"}
                             {:css ".RichContent .ContentItem-action"}])]
     (when (and (exists? driver node)
                (< index 10))
       (scroll-query driver node)
       (when (and (exists? driver comment)
                  (str/includes? (get-element-text driver comment)
                                 "条"))
         (click driver comment)
         (wait 2))
       (when (exists? driver readmore)
         (click driver readmore)
         (wait 1))
       (handle-content (get-element-text driver node))
       (recur driver (+ index 1)))))
  ([driver]
   (zhihu-search-current-page driver 1)))

(defn zhihu-search [driver kword]
  (fill-human driver {:css ".SearchBar-input .Input"} "奔驰")
  (fill driver {:css ".SearchBar-input .Input"} ek/enter)
  (wait 3)
  (zhihu-search-current-page driver))

(defn tieba-content [driver]
  (if (exists? driver {:css ".p_postlist"})
    (some-> (get-element-text driver {:css ".p_postlist"})
            (handle-content)))
  (when (and (exists? driver {:css ".l_posts_num a:nth-last-child(2)"})
             (= (get-element-text driver {:css ".l_posts_num a:nth-last-child(2)"}) "下一页"))
    (click driver {:css ".l_posts_num a:nth-last-child(2)"})
    (wait 2)
    (recur driver)))

(defn tieba-page
  ([driver index]
   (let [node [{:tag :ul :id "thread_list"}
               {:tag :li :class " j_thread_list clearfix" :index index}
               {:css ".threadlist_lz a"}]]
     (when (and (exists? driver node))
       (click driver node)
       (switch-window driver (last (get-window-handles driver)))
       (wait 2)
       (tieba-content driver)
       (close-window driver)
       (recur driver (+ index 1)))))
  ([driver]
   (tieba-page driver 1)))


(defn tieba-search [driver kword]
  (go driver "https://tieba.baidu.com/index.html")
  (fill-human driver {:tag :input :name "kw1"} kword)
  (fill driver {:tag :input :name "kw1"} ek/enter))

; 需要登陆的平台有知乎，百度，微博（能自动登陆）

#_(tieba-search driver "好未来")


(def platforms [:zhihu :tieba :baidu :weibo :souhu])
(def platform-driver-map (reduce #(assoc %1 %2 (search-driver)) {} platforms))
(def platform-login-urls
  {:zhihu "https://www.zhihu.com/signup?next=%2F"
   :tieba "http://tieba.baidu.com/f/user/passport"
   :baidu "https://passport.baidu.com/v2/?login"
   :weibo "https://weibo.com/"
   :souhu "http://www.sohu.com/"})
(def platform-search-handler
  {:zhihu #'zhihu-search
   :tieba #'tieba-search
   :baidu #'baidu-search
   :weibo #'weibo-search
   :souhu #'souhu-search})


#_(click driver {:css ".l_posts_num a:last-child"})

#_(def driver (search-driver))
#_(go driver "http://tieba.baidu.com/p/2860614426")
#_(go driver "https://zhihu.com/")


(defn init-drivers []
  (doseq [[plat driver] platform-driver-map]
    (set-driver-timeout driver)
    (try+
     (go driver (get platform-login-urls plat))
     (catch [:type :etaoin/http-error] _ _))))

(defn quit-drivers []
  (doseq [[_ driver] platform-driver-map]
    (quit driver)))

(defn do-logic []
  (map (fn [n]
         (let [driver (get platform-driver-map n)
               handler (get platform-search-handler n)]
           (future (handler driver "电商之家"))))
       platforms))

(init-drivers)

(do-logic)

(def driver (search-driver))
(set-driver-timeout driver)
(quit-drivers)



#_(fill-human driver {:tag :input :id "TANGRAM__PSP_4__userName"} "18938657523")
#_(fill-human driver {:css "input#TANGRAM__PSP_4__userName"} "111")

#_(go driver "https://zhihu.com/")

#_(save-cookies driver "yesheng" "zhihu")
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


