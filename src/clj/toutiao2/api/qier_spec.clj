(ns toutiao2.api.qier-spec
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.string :as str]
            [toutiao2.api.qier-api :as api]))


(s/def :body/code (s/and string? #(= "0" %)))
(s/def :data/access_token string?)
(s/def :data/expires_in int?)
(s/def :data/openid string?)
(s/def :body/data (s/keys :req-un [:data/access_token :data/expires_in :data/openid]))
(s/def ::body (s/keys :req-un [:body/code :body/data]))


(def source-platform (into #{} (range 1 11)))
(s/def :article/title (s/and string? #(> (count %) 10) #(< (count %) 30)))
(s/def :article/content string?)
(s/def :article/cover_pic (s/and string? #(str/starts-with? % "http")))
(s/def :article/apply (s/nilable int?))
(s/def :article/original_platform #(contains? source-platform %))
(s/def :article/original_url (s/and string? #(str/starts-with? % "http")))
(s/def :article/original_author string?)
(s/def ::article
  (s/keys :req-un [:article/title
                   :article/content
                   :article/cover_pic]
          :opt-un [:article/apply
                   :article/original_platform
                   :article/original_url
                   :article/original_author]))
(s/def :article-ret/code #(= "0" %))
(s/def :article-ret/msg #(= "success" %))
(s/def ::article-ret
  (s/keys :req-un
          [:article-ret/code
           :article-ret/msg]))

(s/fdef api/post-article!
        :args (s/and (s/cat :article :toutiao2.api.qier-spec/article :access-token string?))
        :ret :toutiao2.api.qier-spec/article-ret)

(stest/instrument `toutiao2.api.qier-api/post-article!)