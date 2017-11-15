(ns ^:figwheel-no-load toutiao2.app
  (:require [toutiao2.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
