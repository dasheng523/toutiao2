(ns toutiao2.views
  (:require [toutiao2.subs :as tsubs]
            [toutiao2.utils :as utils]
            [re-frame.core :as rf]
            [re-com.core :refer [v-box h-box button modal-panel input-text label]]
            [reagent.core :as reagent]
            [reagent.core :as r]))

(defn- toutiao-account-item [info]
  [:div {:class "p-1"}
   [:button {:class "btn btn-primary btn-lg"
             :on-click #(rf/dispatch [:set-current-account info])
             :disabled (= (utils/listen [:current-account]) info)}
    "test"]])

(defn func-select-view []
  [v-box
   :children
   [[:h3.mt-1 "选择功能"]
    [h-box
     :children
     [[button
       :label "发文章"
       :class "btn btn-primary m-1"]
      [button
       :label "发商品"
       :class "btn btn-primary m-1"]
      [button
       :label "发视频"
       :class "btn btn-primary m-1"
       :disabled? true]]]]])

(defn account-create-view []
  (if (utils/listen [:toutiao-account-visible?])
    (let [account-name (r/atom "")
          second-step (r/atom false)]
      [modal-panel
       :backdrop-on-click #(rf/dispatch [:show-create-toutiao-account-view false])
       :child [v-box
               :children
               [[label :label "第一步:"]
                [input-text
                 :model account-name
                 :on-change #(reset! account-name %)
                 :placeholder "输入账号名称"]
                [label :label "第二步：" :class "mt-3"]
                [button
                 :label "登录头条"
                 :class "btn btn-primary"
                 :disabled? second-step
                 :on-click #(reset! second-step not)]
                [label :label "第三步：" :class "mt-3"]
                [button
                 :label "保存登录"
                 :class "btn btn-primary"]]]])))

(defn toutiao-account []
  [:div {:class "mt-5"}
   [:h3 "切换账号"]
   [:div {:class "d-flex flex-row mt-1"}
    (for [account (utils/listen [:toutiao-accounts])]
      ^{:key (:id account)}
      [toutiao-account-item account])
    [:div {:class "p-1"}
     [:button {:class "btn btn-primary btn-lg"
               :on-click #(rf/dispatch [:show-create-toutiao-account-view true])}
      "+"]]]
   [account-create-view]])

(defn source-input []
  [:div {:class "mt-5"}
   [:h3 "文章地址"]
   [:textarea {:class "form-control" :rows "5" :placeholder "这里填写文章地址"}]])

(defn start-task-view []
  [:div {:class "mt-5"}
   [:button {:class "btn btn-primary"}
    "开始任务"]])

(defn log-view []
  [:div {:class "mt-5"}
   [:h4 "运行日志"]
   [:p {:class "border text-info p-3"}
    123]])


(defn toutiao-page []
  [:div {:class "container"}
   [:div {:class "row"}
    [:div {:class "col-sm"}
     [func-select-view]
     [toutiao-account]
     [source-input]
     [start-task-view]
     [log-view]]]])