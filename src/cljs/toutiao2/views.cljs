(ns toutiao2.views
  (:require [toutiao2.subs :as tsubs]
            [toutiao2.utils :as utils]
            [re-frame.core :as rf]))

(defn test-view []
  [:div
   [:button {:class "btn btn-primary"
             :on-click #(rf/dispatch [:test-http])} "test1111"]])

(defn- toutiao-account-item [info]
  [:div {:class "p-1"}
   [:button {:class "btn btn-primary btn-lg"
             :on-click #(rf/dispatch [:set-current-account info])
             :disabled (= (utils/listen [:current-account]) info)}
    "test"]])


(defn toutiao-account []
  [:div {:class "mt-1"}
   [:h3 "切换账号"]
   [:div {:class "d-flex flex-row mt-1"}
    (for [account (utils/listen [:toutiao-accounts])]
      ^{:key (:id account)}
      [toutiao-account-item account])
    [:div {:class "p-1"}
     [:button {:class "btn btn-primary btn-lg"}
      "+"]]]])

(defn source-input []
  [:div {:class "mt-1"}
   [:h3 "文章地址"]
   [:textarea {:class "form-control" :rows "5" :placeholder "这里填写文章地址"}]])

(defn start-view []
  [:div {:class "mt-5"}
   [:button {:class "btn btn-primary"}
    "开始任务"]])

(defn log-view []
  [:div {:class "mt-5"}
   [:h4 "运行日志"]
   [:p {:class "border text-info p-3"}
    123]])

(defn modal-component []
  [:div {:class "modal"}
   [:div {:class "modal-dialog"}
    [:div {:class "modal-content"}
     [:div {:class "modal-header"}
      [:h5 {:class "modal-title"}]
      [:button {:class "close"}
       [:span "&times;"]]]
     [:div {:class "modal-body"}
      [:p "1111"]]
     [:div {:class "modal-footer"}
      [:button {:class "btn btn-primary"} "Save"]
      [:button {:class "btn btn-secondary"} "close"]]]]])

(defn toutiao-page []
  [:div {:class "container"}
   [:div {:class "row"}
    [:div {:class "col-sm"}
     [toutiao-account]
     [source-input]
     [start-view]
     [log-view]
     [modal-component]]]])