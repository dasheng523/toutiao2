(ns toutiao2.fulltext.index
  (:require [toutiao2.tuding.search-engine :as search])
  (:import (com.hankcs.hanlp HanLP)
           (com.hankcs.hanlp.suggest Suggester)
           (com.hankcs.hanlp.dictionary CoreSynonymDictionary)))

(-> (HanLP/extractKeyword "程序员(英文Programmer)是从事程序开发、维护的专业人员。一般将程序员分为程序设计人员和程序编码人员，但两者的界限并不非常清楚，特别是在中国。软件从业人员分为初级程序员、高级程序员、系统分析员和项目经理四大类。" 100)
    count)



(-> (search/google-search "全球首款人工智能手机芯片是?")
    (HanLP/extractKeyword 100))


(filter #{1 2} [1  3 4])



(HanLP/extractKeyword "签约仪式前，秦光荣、李纪恒、仇和等一同会见了参加签约的企业家" 5)

(.seg (HanLP/newSegment) "签约仪式前，秦光荣、李纪恒、仇和等一同会见了参加签约的企业家")

(def suggester (Suggester.))
(doseq [s ["威廉王子发表演说 呼吁保护野生动物\n"
           "《时代》年度人物最终入围名单出炉 普京马云入选\n"
           "“黑格比”横扫菲：菲吸取“海燕”经验及早疏散\n"
           "日本保密法将正式生效 日媒指其损害国民知情权\n"
           "英报告说空气污染带来“公共健康危机”"]]
  (.addSentence suggester s))

(.suggest suggester "mayun" 1)

(HanLP/extractPhrase "目前国内从事算法研究的工程师不少，但是高级算法工程师却很少，是一个非常紧缺的专业工程师。算法工程师根据研究领域来分主要有音频/视频算法处理、图像技术方面的二维信息算法处理和通信物理层、雷达信号处理、生物医学信号处理等领域的一维信息算法处理。"
                     1000)


; 提取搜索结果HTML中的所有关键词语
; 分析答案选项，提取关键词
; 从搜索结果关键词语中，找出答案选项相关的词语，并统计这些词语的个数

(def words ["味道",
            "香味"
            "气味"
            "臭味"])

(for [i words
      j words]
  [i j (CoreSynonymDictionary/distance i j)])

(HanLP/extractSummary (search/baidu-search "忽如一夜春风来 千树万树梨花开") 5)
