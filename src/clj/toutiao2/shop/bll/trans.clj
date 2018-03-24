(ns toutiao2.shop.bll.trans
  (:require [tongue.core :as tongue]))


(def trans-dict
  (atom {:en {:fail "fail"
              :flower "Flower"}

         :cn {}

         :fr {}}))

(defn load-dict
  "加载翻译字典"
  []
  (reset! trans-dict
          {:en {}

           :cn {}

           :fr {}}))

(def __
  (tongue/build-translate @trans-dict))


