(ns toutiao2.zawu.bocai
  (:require [toutiao2.driver :as tdriver]
            [etaoin.api :refer :all]))

(def driver (tdriver/create-default-browser))

(go driver "https://www.365-838.com/#/HO/")
(click driver [{:tag :nav :class "hm-BigButtons_Inner "}
               {:tag :a :index 2}])

(click driver [{:css "div.ipo-ClassificationBarScrollable_ScrollContentNoAnimation"}
               {:tag :div :index 4}])

(click driver {:css "div.ipo-ScoreDisplayStandard_Wrapper"})
