(ns toutiao2.shop.bll.trans
  (:require [tongue.core :as tongue]))


(def trans-dict
  (atom {:en {:auth-fail "fail"
              :flower "Flower"}

         :cn {:auth-fail "您的用户名或密码有误，请重试"}

         :fr {}
         :tongue/fallback :en}))

(defn load-dict
  "加载翻译字典"
  []
  (reset! trans-dict
          {:en {}

           :cn {}

           :fr {}}))

(def tr
  (tongue/build-translate @trans-dict))



