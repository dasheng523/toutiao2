(ns toutiao2.zawu.niushidatest
  (:require [toutiao2.zawu.niushida :as sut]
            [etaoin.api :refer :all]
            [clojure.string :as str]
            [clojure.test :as t]))

(def driver (sut/search-driver))
(sut/set-driver-timeout driver)

(sut/load-config)
@sut/result-container
(count @sut/result-container)

(sut/reset-app)
@sut/task-futures

(doseq [k ["纽仕达" "电商之家"]]
  (sut/tieba-allba-search driver k))


(sut/tieba-allba-search-page driver)

#_(when (and
       (exists? driver {:css "a.next"})
       (str/includes? (get-element-text driver {:css "a.next"}) "下一页"))
  (click driver {:css "a.next"})
  (wait 3)
  #_(recur (+ n 1)))


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


