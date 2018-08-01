(ns toutiao2.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [toutiao2.ajax :refer [load-interceptors!]]
            [toutiao2.events]
            [toutiao2.subs]
            [toutiao2.views :refer [toutiao-page common-page]]
            [toutiao2.websocket])
  (:import goog.History))

(defn nav-link [uri title page]
  (let [selected-page (rf/subscribe [:page])]
    [:li.nav-item
     {:class (when (= page @selected-page) "active")}
     [:a.nav-link {:href uri} title]]))


(defn navbar []
  (r/with-let
    [show? (r/atom false)]
    (fn []
      [:nav.navbar {:class "navbar-expand-lg navbar-dark bg-primary"}
       [:a.navbar-brand {:href "#"} "Home"]
       [:button.navbar-toggler
        {:on-click #(swap! show? not)}
        [:span.navbar-toggler-icon]]
       [:div {:class (str "collapse navbar-collapse " (when @show? "show"))}
        [:ul.navbar-nav
         [nav-link "#/" "Home" :home]
         [nav-link "#/toutiao" "头条" :toutiao]
         [nav-link "#/about" "About" :about]
         [nav-link "#/login" "Login" :login]
         [nav-link "#/common" "爬虫" :common]]]])))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src (str js/context "/img/warning_clojure.png")}]]]])


(defn home-page []
  [:div.container
   (when-let [docs @(rf/subscribe [:docs])]
     [:div.row>div.col-sm-12
      [:div {:dangerouslySetInnerHTML
             {:__html (md->html docs)}}]])])

(defn login-page []
  [:div.container.mt-5
   [:div.row>div.col-sm-12
    [:div.card
     [:div.card-header "登录"]
     [:div.card-body
      [:form
       [:div.form-group
        [:label "用户名"]
        [:input.form-control {:placeholder "请输入用户名"}]]
       [:div.form-group
        [:label "密码"]
        [:input.form-control {:placeholder "请输入密码" :type :password}]]
       [:button.btn.btn-primary "登录"]]]]]])

(def pages
  {:home #'home-page
   :about #'about-page
   :toutiao #'toutiao-page
   :login #'login-page
   :common #'common-page})

(defn page []
  [:div
   [navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(defn- simple-defroute [kw]
  (secretary/defroute (str "/" (name kw)) []
    (rf/dispatch [:set-active-page kw])))

(defn- init-routes []
  (let [route-keys (keys pages)]
    (doseq [k route-keys]
      (simple-defroute k))))

(secretary/defroute "/" []
  (rf/dispatch [:set-active-page :home]))

(init-routes)

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET "/docs" {:handler #(rf/dispatch [:set-docs %])}))

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
