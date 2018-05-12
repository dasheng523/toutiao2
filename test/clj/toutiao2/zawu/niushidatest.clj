(ns toutiao2.zawu.niushidatest
  (:require [toutiao2.zawu.niushida :as sut]
            [etaoin.api :refer :all]
            [clojure.string :as str]
            [clojure.test :as t]))

(def driver (sut/search-driver))

(sut/load-config)
@sut/result-container
(count @sut/result-container)

@sut/task-futures

(doseq [k ["纽仕达" "电商之家"]]
  (sut/zhihu-search driver k))

(swap! sut/task-futures assoc :a :a)


(quit driver)

(get-element-text driver {:css ".tail-info:nth-child(3)"})
(switch-window driver (last (get-window-handles driver)))

(reset! sut/result-container #{})
(sut/result-page 1 10)
(sut/markbad-list)

(sut/quit-drivers sut/driver-map)

(get-title driver)

(if (exists? driver {:css ".p_postlist"})
  (get-element-text driver {:css ".p_postlist"}))


