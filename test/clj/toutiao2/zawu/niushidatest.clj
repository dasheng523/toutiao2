(ns toutiao2.zawu.niushidatest
  (:require [toutiao2.zawu.niushida :as sut]
            [etaoin.api :refer :all]
            [clojure.test :as t]))

(def driver (sut/search-driver))

(sut/load-config)
@sut/result-container
(count @sut/result-container)

(sut/tieba-search driver "纽仕达")

(quit driver)

(sut/quit-drivers sut/driver-map)
