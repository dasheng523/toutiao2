(ns toutiao2.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [toutiao2.core-test]))

(doo-tests 'toutiao2.core-test)

