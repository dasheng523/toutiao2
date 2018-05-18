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
            [slingshot.slingshot :refer [throw+ try+]]
            [toutiao2.utils :as utils]))

(defonce niushida-config (atom nil))

(def tieba-limit-page 5)
(def zhihu-limit-count 50)
(def weibo-limit-page 5)
(def baidu-limit-page 6)
(def souhu-limit-count 50)

(def driver-timeout (* 15 1000))

(defmacro with-try [& body]
  `(try+
   ~@body
   (catch [:type :etaoin/http-error] _# _#)))

(defn load-config []
  (let [path (if (isWindows?)
               (-> config/env :win-dired-path)
               (-> config/env :mac-dired-path))]
    (-> (slurp (str path "/niushida-config.json"))
        (json/parse-string true)
        (->> (reset! niushida-config)))))


(defn- badwords []
  (:badwords @niushida-config))


(defn- has-badwords? [s]
  (some #(str/includes? s %)
        (badwords)))

(defn- main-words []
  (:mainwords @niushida-config))

(defn- extra-words []
  (:extrawords @niushida-config))

(defn- search-words []
  (for [main (main-words)
        extra (extra-words)]
    (str main " " extra)))

(defonce result-container (atom #{}))

(defn handle-content [url title platform content pagetime]
  (let [bads (badwords)
        badw (doall (filter #(str/includes? content %) bads))]
    (when (not-empty badw)
      (println url)
      (swap! result-container conj
             {:url url
              :badword badw
              :id (utils/rand-idstr)
              :title title
              :pagetime (or pagetime "暂无")
              :ctime (System/currentTimeMillis)
              :platform platform}))))

(defn search-driver []
  (chrome
   {:path-driver (.getPath (io/resource (tdriver/get-chromedriver-path)))
    :args []
    :args-driver ["--ipc-connection-timeout=1"]
    :size [1920 800]}))

(defn set-driver-timeout
  ([driver timeout]
   (with-resp driver :post
     [:session (:session @driver) :timeouts]
     {:type "page load" :ms timeout}
     _))
  ([driver]
   (set-driver-timeout driver driver-timeout)))

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
  (wait 2)
  (loop [i 1]
    (let [detail-node [{:tag :div :class "ArticleFeed"}
                       {:tag :div :index i}
                       {:tag :div :class "ant-card-body"}
                       {:tag :a}]]
      (when (and (exists? driver detail-node)
                 (< i souhu-limit-count))
        (scroll-query driver detail-node)
        (click driver detail-node)
        (wait 1)
        (switch-window driver (last (get-window-handles driver)))
        (wait 5)
        (handle-content (get-url driver)
                        (get-title driver)
                        :souhu
                        (souhu-content driver)
                        (if (exists? driver {:css ".time"})
                          (get-element-text driver {:css ".time"})))
        (close-window driver)
        (switch-window driver (first (get-window-handles driver)))
        (recur (+ i 1))))))


(defn baidu-current-page
  ([driver index]
   (let [node [{:tag :div :class "result c-container " :index index}
               {:tag :h3}
               {:tag :a}]]
     (when (exists? driver node)
       (scroll-query driver node)
       (scroll-by driver 0 -50)
       (try+
        (click driver node)
        (wait 1)
        (switch-window driver (last (get-window-handles driver)))
        (wait 3)
        (wait-exists driver {:css "body"})
        (handle-content (get-url driver)
                        (get-title driver)
                        :baidu
                        ((dispatch-content-fn (get-url driver)) driver)
                        nil)
        (catch [:type :etaoin/timeout] _ _)
        (catch Object _ _))
       (close-window driver)
       (switch-window driver (first (get-window-handles driver)))
       (recur driver (+ index 1)))))
  ([driver]
   (baidu-current-page driver 1)))


(defn baidu-search-keyword [driver searchkey]
  (go driver "https://www.baidu.com")
  (fill driver {:css "input.s_ipt"} searchkey ek/enter)
  (wait 2)
  (dotimes [n baidu-limit-page]
    (baidu-current-page driver)
    (when (exists? driver {:css "div#page > a:last-child"})
      (click driver {:css "div#page > a:last-child"})
      (wait 2))))

(defn baidu-search [driver searchkey]
  (let [extra-words (:extrawords @niushida-config)]
    (doseq [word extra-words]
      (baidu-search-keyword driver (str searchkey " " word)))))

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
   (wait 2)
   (let [node [{:tag :div :class "WB_cardwrap S_bg2 clearfix" :index index}]
         readmore (conj node {:tag :a :class "WB_text_opt"})
         timenode (into node [{:tag :div :class "feed_from W_textb"}
                              {:tag :a}])
         author (conj node {:tag :a :class "W_texta W_fb"})
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
       (handle-content (get-url driver)
                       "微博"
                       :weibo
                       (get-element-text driver node)
                       (if (exists? driver timenode)
                         (get-element-text driver timenode)))
       (recur driver (+ index 1)))))
  ([driver]
   (weibo-search-current-page driver 1)))

(defn weibo-search [driver kword]
  (fill-human driver {:css "input.W_input"} kword)
  (with-try
    (fill driver {:css "input.W_input"} ek/enter))
  (wait 2)
  (dotimes [n weibo-limit-page]
    (weibo-search-current-page driver)
    (when (and (exists? driver {:tag :a :class "page next S_txt1 S_line1"})
               (get-element-text driver
                                 {:tag :a :class "page next S_txt1 S_line1"}))
      (with-try (click driver {:tag :a :class "page next S_txt1 S_line1"}))
      (wait 3))))

(defn zhihu-search-current-page
  ([driver index]
   (let [node [{:tag :div :class "Card"} {:tag :div :class "List-item" :index index}]
         readmore (into node [{:css ".RichContent .ContentItem-more"}])
         comment (into node [{:css ".ContentItem"}
                             {:css ".RichContent .ContentItem-action"}])
         urlnode (into node [{:css ".ContentItem-title a"}])
         titlenore (into node [{:css ".ContentItem-title"}])
         timenode (into node [{:css ".ContentItem-time a span"}])
         bads (badwords)]
     (when (and (exists? driver node)
                (< index zhihu-limit-count))
       (scroll-query driver node)
       (when (and (exists? driver comment)
                  (str/includes? (get-element-text driver comment)
                                 "条"))
         (click driver comment)
         (wait 2))
       (when (exists? driver readmore)
         (click driver readmore)
         (wait 1))
       (handle-content (get-element-attr driver urlnode :href)
                       (get-element-text driver titlenore)
                       :zhihu
                       (get-element-text driver node)
                       (if (exists? driver timenode)
                         (get-element-text driver timenode)))
       (recur driver (+ index 1)))))
  ([driver]
   (zhihu-search-current-page driver 1)))


(defn zhihu-search [driver kword]
  (with-try
    (go driver (str "https://www.zhihu.com/search?type=content&q=" kword)))
  (wait 3)
  (zhihu-search-current-page driver))


(defn tieba-content [driver]
  (if (exists? driver {:css ".p_postlist"})
    (some-> (get-element-text driver {:css ".p_postlist"})
            (#(handle-content (get-url driver)
                              (get-title driver)
                              :tieba
                              %
                              (if (exists? driver {:css ".tail-info:nth-child(3)"})
                                (get-element-text driver {:css ".tail-info:nth-child(3)"}))))))
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
       (wait 1)
       (click driver node)
       (switch-window driver (last (get-window-handles driver)))
       (wait 2)
       (tieba-content driver)
       (close-window driver)
       (switch-window driver (first (get-window-handles driver)))
       (recur driver (+ index 1)))))
  ([driver]
   (tieba-page driver 1)))


(defn tieba-search [driver kword]
  (go driver "https://tieba.baidu.com/index.html")
  (fill-human driver {:tag :input :name "kw1"} kword)
  (fill driver {:tag :input :name "kw1"} ek/enter)
  (wait 3)
  (loop [n tieba-limit-page]
    (tieba-page driver)
    (when (and
           (exists? driver {:css "a.next"})
           (str/includes? (get-element-text driver {:css "a.next"}) "下一页"))
      (click driver {:css "a.next"})
      (wait 3)
      (recur (+ n 1)))))


(defn tieba-allba-search-page
  ([driver index]
   (when (exists? driver [{:class "s_post" :index index}
                          {:css ".p_title a"}])
     (scroll-query driver [{:class "s_post" :index index}
                           {:css ".p_title a"}])
     (scroll-by driver 0 -50)
     (click driver [{:class "s_post" :index index}
                    {:css ".p_title a"}])
     (wait 3)
     (switch-window driver (last (get-window-handles driver)))
     (tieba-content driver)
     (wait 1)
     (close-window driver)
     (switch-window driver (first (get-window-handles driver)))
     (wait 1)
     (recur driver (+ 1 index))))
  ([driver]
   (tieba-allba-search-page driver 1)))

(defn tieba-allba-search [driver kword]
  (go driver "https://tieba.baidu.com/index.html")
  (fill-human driver {:css ".search_ipt"} "PHP")
  (click driver {:css ".j_search_post"})
  (loop [n 10]
    (when (< n 10)
      (wait 3)
      (tieba-allba-search-page driver)
      (wait 1)
      (when (and
             (exists? driver {:css "a.next"})
             (str/includes? (get-element-text driver {:css "a.next"}) "下一页"))
        (click driver {:css "a.next"})
        (wait 3)
        (recur (+ n 1))))))


;; 需要登陆的平台有知乎，百度，微博（能自动登陆）
(defonce platforms (atom #{}))

(defn set-platforms [k del]
  (if del
    (swap! platforms disj k)
    (swap! platforms conj k)))

(defn create-driver-map []
  (reduce #(assoc %1 %2 (search-driver)) {} @platforms))


(def platform-login-urls
  {:zhihu "https://www.zhihu.com/signup?next=%2F"
   :tieba "http://tieba.baidu.com/f/user/passport"
   :baidu "https://passport.baidu.com/v2/?login"
   :weibo "https://weibo.com/"
   :souhu "http://search.sohu.com/"})


(def platform-search-handler
  {:zhihu #'zhihu-search
   :tieba #'tieba-search
   :baidu #'baidu-search
   :weibo #'weibo-search
   :souhu #'souhu-search})


(defn init-platform [driver plat]
  (set-driver-timeout driver)
  (try+
   (go driver (get platform-login-urls plat))
   (catch [:type :etaoin/http-error] _ _)))

(defn quit-drivers [driver-map]
  (doseq [[_ driver] driver-map]
    (quit driver)))


(defonce driver-map (atom nil))
(defonce task-futures (atom {}))

(defn search-multi-driver [driver-map kwords]
  (doseq [plat @platforms]
    (let [driver (get driver-map plat)
          handler (get platform-search-handler plat)
          ft (future (doseq [kword kwords] (handler driver kword)))]
      (swap! task-futures assoc plat ft))))

(defn reset-app []
  (when @driver-map
    (quit-drivers @driver-map))
  (reset! driver-map #{})
  (reset! result-container #{})
  (reset! platforms #{})
  (when @task-futures
    (doseq [f @task-futures]
      (future-cancel (second f))))
  (reset! task-futures {}))


(defn init-app []
  (load-config)
  (reset! driver-map
          (reduce
           #(let [driver
                  (or (get @driver-map %2)
                      (let [dd (search-driver)]
                        (init-platform dd %2)
                        dd))]
              (assoc %1 %2 driver))
           {}
           @platforms)))


#_(reset-app)

(defn start-app [kwords]
  (if (> (count @platforms) 0)
    (do (reset! result-container #{})
        (search-multi-driver @driver-map kwords))))

(defn stop-app []
  (when @driver-map
      (quit-drivers @driver-map)
    (reset! driver-map nil))
  (when @task-futures
    (doseq [f @task-futures]
      (future-cancel (second f)))))

(defn app-status []
  (map #(identity {:platform (name %)
                   :status (condp = (if-let [f (get @task-futures %)]
                                      (future-done? f)
                                      nil)
                             true "已完成"
                             false "正在进行"
                             nil "未开始")})
       @platforms))


(defonce badinfos (atom #{}))

(defn mark-bad [id del?]
  (if del?
    (swap! badinfos disj id)
    (swap! badinfos conj id)))


(defn result-page [page size]
  (let [mun (count @result-container)]
    (some-> (sort #(compare (:ctime %1) (:ctime %2))
              @result-container)
            (->> (into []))
            (subvec (min (- mun 1) (* page size))
                    (min mun (* (+ 1 page) size)))
            (->> (map #(if (some #{(get % :id)} @badinfos)
                     (assoc % :isbad true)
                     (assoc % :isbad false)))))))

(def excel-fileds [:title :url :platform :pagetime])

(defn markbad-list []
  (let [list (filter #(some #{(get % :id)} @badinfos)
                     @result-container)]
    (cons (map name excel-fileds)
          (map (fn [item]
                 (map #(str (get item %)) excel-fileds))
               list))))

(defn create-markbad-file []
  (let [f (str (config/get-download-path) "/" (utils/rand-idstr) ".xlsx")]
    (utils/save-raw-data-excel (markbad-list) f)
    f))

