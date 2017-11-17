(ns toutiao2.utils
  (:require [re-frame.core :as rf]))

(defn listen [k]
  @(rf/subscribe k))