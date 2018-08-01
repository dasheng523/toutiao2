(ns toutiao2.views
  (:require [toutiao2.subs :as tsubs]
            [toutiao2.utils :as utils]
            [re-frame.core :as rf]
            [re-com.core :refer [v-box h-box button modal-panel input-text label]]
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




;;;;;;;;;;;;;;;;;;;;;;;; Common Table

(def table-schema
  {:cols [[:col1 "col1"] [:col2 "col22"] [:col3 "col33"]]
   :data [{:col1 "data1" :col2 "data2" :col3 "data3" :id "1"}
          {:col1 "data1" :col2 "data2" :col3 "data3" :id "2"}]
   :actions [:edit :delete {:text "查看" :method "view"}]
   :maxpage 10
   :total 51})

(defn- col-view [cols]
  [:tr
   [:th {:width 50} " "]
   (for [col cols]
     ^{:key (first col)}
     [:th (last col)])
   [:th {:width 150} "操作"]])

(defn- action-view [item actions]
  [:td
   (for [action actions]
     (condp = action
       :edit
       ^{:key action}[:button {:class "btn btn-link btn-sm"} "编辑"]
       :delete
       ^{:key action}[:button {:class "btn btn-link btn-sm"
                               :on-click #(if (js/confirm "确定要删除吗？")
                                            (js/alert (str "已删除" (:id item))))} "删除"]
       ^{:key (:method action)}[:button {:class "btn btn-link btn-sm"} (:text action)]))])

(defn- item-view [{:keys [item cols actions on-select-id]}]
  [:tr
   [:td [:input {:type "checkbox" :on-change #(on-select-id (:id item))}]]
   (for [col cols]
     ^{:key (get item col)}
     [:td (get item col)])
   [action-view item actions]])

(defn- table-head-view []
  [:div {:class "row"}
   [:div {:class "col-md-3"}
    [:div {:class "input-group"}
     [:div {:class "input-group-prepend"}
      [:label {:class "input-group-text"} "操作"]]
     [:select {:class "custom-select"}
      [:option "选择操作"]
      [:option "本页全选"]
      [:option "批量删除"]]]]
   [:div {:class "col-md-5"}
    [:button {:class "btn btn-link"} "刷新"]
    [:button {:class "btn btn-primary"} "新增"]]
   [:div {:class "col-md-4"}
    [:div {:class "input-group"}
     [:input {:type "text" :class "form-control" :placeholder "请输入关键词"}]
     [:div {:class "input-group-append"}
      [:button {:class "btn btn-primary"} "搜索"]]]]])

(defn- page-view [current maxpage setpage-fn]
  [:nav
   [:ul {:class "pagination"}
    [:li {:class (if (= current 1) "page-item disabled" "page-item")}
     [:button {:class "page-link" :dangerouslySetInnerHTML {:__html "&laquo;"} :on-click (partial setpage-fn (- current 1))}]]
    (let [mn (max (if (< (+ current 3) maxpage) (- current 3) (- maxpage 6)) 1)
          mx (min (max (+ current 4) 8) (+ maxpage 1))]
      (for [n (range mn mx)]
        ^{:key n}
        [:li {:class (str "page-item" (if (= current n) " active"))} [:button {:class "page-link" :on-click (partial setpage-fn n)} n]]))
    [:li {:class (if (= current maxpage) "page-item disabled" "page-item") :on-click (partial setpage-fn (+ current 1))}
     [:button {:class "page-link" :dangerouslySetInnerHTML {:__html "&raquo;"}}]]]])


(defn table-view [schema]
  (let [current (r/atom 1)
        selected-ids (r/atom #{})
        ceve #(reset! current %)
        on-select-id #(swap! selected-ids conj %)
        on-select-all #(-> (map :id (:data schema))
                           (->> (swap! selected-ids into)))]
    (fn []
      [:div {:class "card"}
       [:div {:class "card-header"}
        [table-head-view]]
       [:div {:class "card-body"}
        [:table {:class "table table-bordered table-hover"}
         [:thead
          [col-view (:cols schema)]]
         [:tbody
          (for [item (:data schema)]
            ^{:key (:id item)}
            [item-view {:item item
                        :cols (map first (:cols schema))
                        :actions (:actions schema)
                        :on-select-id on-select-id
                        :on-select-all on-select-all}])]]]
       [:div {:class "card-footer"}
        [:div {:class "row"}
         [:div {:class "col-md-8"} [page-view @current (:maxpage schema) ceve]]
         [:div {:class "col-md-4"} (str "总共 " (:total schema) " 条记录")]]]])))

(defn common-page []
  [:div {:class "container"}
   [:div {:class "rows"}
    [:div {:class "col-sm"}
     [table-view table-schema]]]])
