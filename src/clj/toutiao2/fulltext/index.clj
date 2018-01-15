(ns toutiao2.fulltext.index
  (:require [clj-bosonnlp.core :as bos]))

(bos/initialize "wcY5KVg5.21955.F8s57q6YTp36")

(bos/depparser "植物的香味")

(bos/depparser "邓紫棋谈男友林宥嘉：我觉得我比他唱得好")

(bos/suggest "2个")
