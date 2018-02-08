(ns toutiao2.ave40.spinner
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [opennlp.nlp :as nlp]
            [toutiao2.ave40.db :refer :all]))

(def get-sentences (nlp/make-sentence-detector "resources/language_model/en-sent.bin"))

(defn- split-content-len [content maxlen hold-word]
  (reduce (fn [v sen]
            (let [last-sen (last v)
                  add-sen (str last-sen sen)]
              (if (< (count add-sen) maxlen)
                (conj (vec (drop-last v)) add-sen)
                (conj v sen))))
          []
          (get-sentences (str/replace content #"\n" hold-word))))



(defn- spinner-login []
  (let [resp (-> (http/post "http://thebestspinner.com/api.php"
                            {:form-params {:action "authenticate"
                                           :format "xml"
                                           :username "515462418@qq.com"
                                           :password "40U30600U0034383W"}})
                 :body)
        session (re-find #"<session>([\S\s]*?)</session>" resp)]
    (if session (second session) (throw (Exception. "session is null")))))


(defn- spinner-post [params]
  (let [resp (-> (http/post "http://thebestspinner.com/api.php"
                            {:form-params params})
                 :body)
        output (re-find #"<output>([\S\s]*?)</output>" resp)]
    (if output (second output)
               (throw (Exception. (str resp))))))

(defn- spinner-synonyms [text session]
  (spinner-post {:action "identifySynonyms"
                 :session session
                 :format "xml"
                 :text text}))

(defn- spinner-sentences [text session]
  (spinner-post {:action "rewriteSentences"
                 :session session
                 :format "xml"
                 :text text}))

(defn- spinner-randomSpin [text session]
  (spinner-post {:action "randomSpin"
                 :session session
                 :format "xml"
                 :text text}))

(defn- spinner-parse [session text]
  (->
    text
    (spinner-synonyms session)
    (spinner-randomSpin session)
    (spinner-sentences session)
    (spinner-randomSpin session)))

(defn create-spinner []
  (let [session (spinner-login)]
    (partial spinner-parse session)))

(defn spin-and-save-one-article [spinner article]
  (try
    (let [title (spinner (:title article))
          pies (split-content-len(:article article) 3000 "-----")
          content (-> pies
                      (->> (map (fn [p] (if p (spinner p)))))
                      (->> (str/join ""))
                      (str/replace #"-----" "\n"))]
      (println (:title article))
      (update-data
        article-db
        {:table "articles"
         :updates {:spinner_title title :spinner_article content}
         :where (str "id=" (:id article))}))
    (catch Exception e
      (log/error e))))

(defn spin-and-save-articles [articles]
  (let [spinner (create-spinner)]
    (doseq [article articles]
      (spin-and-save-one-article spinner article))))

(defn simple-run-spinner-task []
  (spin-and-save-articles
    (select-all article-db
                {:table "articles" :where (str "isnull(spinner_title) and not isnull(article) and article <> '' and "
                                               "source_url like 'https://pegfitzpatrick.com%' "
                                               "limit 50")}))
  (spin-and-save-articles
    (select-all article-db
                {:table "articles" :where (str "isnull(spinner_title) and not isnull(article) and article <> '' and "
                                               "source_url like 'http://www.autoexpress.co.uk%' "
                                               "limit 50")}))
  (spin-and-save-articles
    (select-all article-db
                {:table "articles" :where (str "isnull(spinner_title) and article <> '' and "
                                               "source_url like 'http://www.vogue.co.uk%' "
                                               "limit 50")}))

  (spin-and-save-articles
    (select-all article-db
                {:table "articles" :where (str "isnull(spinner_title) and article <> '' and "
                                               "source_url like 'https://greatist.com%' "
                                               "limit 50")}))
  (spin-and-save-articles
    (select-all article-db
                {:table "articles" :where "isnull(spinner_title) and article <> ''"})))
