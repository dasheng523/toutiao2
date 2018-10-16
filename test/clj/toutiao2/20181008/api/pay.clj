(ns toutiao2.api.pay
  (:require [clj-http.client :as http]
            [toutiao2.api.cache :as cache]
            [cheshire.core :as json]))

(defn make-paypal-config [client-id secret return-url cancel-url is-sandbox]
  {:client-id client-id
   :secret secret
   :is-sandbox is-sandbox
   :return-url return-url
   :cancel-url cancel-url})

(defn- paypal-domain [is-sandbox]
  (if is-sandbox
    "https://api.sandbox.paypal.com"
    "https://api.paypal.com"))

(defn- paypal-token-api [{:keys [client-id secret is-sandbox]}]
  (http/post (str (paypal-domain is-sandbox) "/v1/oauth2/token")
             {:accept :json
              :as :json
              :headers {"Accept-Language" "en_US"
                        "Accept" "application/json"}
              :basic-auth (str client-id ":" secret)
              :form-params {:grant_type "client_credentials"}}))

(def cache (cache/make-cache))

(defn paypal-get-token [config]
  (when (empty? (cache/get-cache cache :token-info))
    (let [token-rs (paypal-token-api config)]
      (cache/set-cache cache
                       :token-info
                       (-> token-rs :body)
                       (-> token-rs :body :expires_in))))
  (cache/get-cache cache :token-info))

(defn paypal-make-order [{:keys [return-url cancel-url is-sandbox] :as config}
                         {:keys [total description order-no currency] :or {currency "USD" description "paypal"}}]
  (let [body (json/generate-string {:intent "sale"
                                    :redirect_urls {:return_url return-url
                                                    :cancel_url cancel-url}
                                    :payer {:payment_method "paypal"}
                                    :transactions [{:amount {:total total
                                                             :currency currency}
                                                    :description description
                                                    :invoice_number order-no}]})
        rs (http/post (str (paypal-domain is-sandbox) "/v1/payments/payment")
                      {:as :json
                       :headers {"Authorization" (str "Bearer " (:access_token (paypal-get-token config)))
                                 "Content-Type" "application/json"}
                       :body body})]
    (-> rs :body)))

(defn paypal-get-order [{:keys [is-sandbox] :as config}
                        paypal-order-id]
  (let [rs (http/get (str (paypal-domain is-sandbox) "/v1/payments/payment/" paypal-order-id)
                     {:headers {"Authorization" (str "Bearer " (:access_token (paypal-get-token config)))
                                "Content-Type" "application/json"}
                      :as :json})]
    (-> rs :body)))

(defn paypal-verify-order [config paypal-order-id]
  (let [rs (paypal-get-order config paypal-order-id)]
    (= "approved" (-> rs :state))))



#_(let [config (make-paypal-config
              "AaVet-7q_zeqVqHOexsZYgqfv-iuJibLccL3IyX5CCgs2J-h9DYWgQEOeaipIvuOcgxDi5_ClaYdzp5h"
              "ENafRdWMAgjlzrpjCWeCCWjDrptfHEaaEYEnbt_OjAf6GSQqCUkJZuR75VjjuYYdrAyFQj-YG-d7wC4T"
              "http://www.baidu.com"
              "http://www.baidu.com"
              true)]
  (println (paypal-verify-order config
                                "PAY-2BN80114JY5089721LKJL6TQ")))

