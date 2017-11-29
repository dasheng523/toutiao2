(ns toutiao2.events
  (:require [toutiao2.db :as db]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [ajax.core :as ajax]
            [day8.re-frame.http-fx]))

(defn- create-default-http-xhrio [url data on-success on-failure]
  {:method          :post
   :uri             url
   :params          data
   :timeout         3000
   :format          (ajax/json-request-format)
   :response-format (ajax/json-response-format {:keywords? true})
   :on-success      on-success
   :on-failure      on-failure})


(defn- reg-simple-event [event-key db-key]
  "注册一个将事件内容放入到DB的事件"
  (reg-event-db
    event-key
    (fn [db [_ page]]
      (assoc db db-key page))))

(defn- reg-simple-talk-server-event
  "注册一个发送HTTP请求的事件"
  [event-key http-xhrio]
  (reg-event-fx
    event-key
    (fn
      [{db :db} _]
      {:http-xhrio http-xhrio
       :db  (assoc db :loading? true)})))

(defn- reg-ajax-event
  "注册处理ajax数据的事件"
  [event-key handler]
  (reg-event-db
    event-key
    (fn [db [_ response]]
      (-> db
          (assoc :loading? true)
          (handler (js->clj response))))))

(defn- reg-simple-ajax-event
  "注册事件，简单的将ajax返回数据放到db里面"
  [event-key db-key]
  (reg-ajax-event
    event-key
    #(assoc %1 db-key %2)))

;;;;;;;;;;;;;;;;;;;; Reg Events
;; 初始化事件
(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

;; 默认显示错误的事件
(reg-ajax-event
  :default-show-error
  (fn [db _]
    (js/alert (:message "发送请求失败"))))

;; 跳页事件
(reg-simple-event :set-active-page :page)
;; 读文档事件
(reg-simple-event :set-docs :docs)
(reg-simple-event :show-create-toutiao-account-view :toutiao-account-visible?)
(reg-simple-ajax-event :init-toutiao-accounts :toutiao-accounts)
(reg-simple-event :set-current-account :current-account)

(reg-simple-talk-server-event
  :test-http
  (create-default-http-xhrio
    "http://localhost:3000/api/minus"
    {:x 5 :y 1}
    [:init-toutiao-accounts]
    [:default-show-error]))