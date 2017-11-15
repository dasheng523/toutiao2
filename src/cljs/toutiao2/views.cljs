(ns toutiao2.views
  (:require [toutiao2.subs :as tsubs]
            [re-frame.core :as rf]))

(defn- toutiao-account-item [info]
  [:tr
   [:td (:name info)]
   [:td [:button {:class "btn btn-outline-primary"} "切换"]]])


(defn toutiao-account []
  [:div {:class "table-responsive"}
   [:table {:class "table"}
    [:thead
     [:tr
      [:th "Name"]
      [:th "Action"]]]
    [:tbody
     (for [account (tsubs/listen [:toutiao-account])]
       ^{:key (:id account)}
       [toutiao-account-item account])]]])