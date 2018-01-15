(ns toutiao2.fulltext.index
  (:require [clj-bosonnlp.core :as bos]))

(bos/initialize "wcY5KVg5.21955.F8s57q6YTp36")

(bos/depparser "植物的香味")

(bos/suggest "粉丝")
