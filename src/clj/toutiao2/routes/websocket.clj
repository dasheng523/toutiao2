(ns toutiao2.routes.websocket
  (:require [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit      :refer (get-sch-adapter)]
            [taoensso.timbre :as timbre :refer-macros (tracef debugf infof warnf errorf)]
            [compojure.core :refer [defroutes GET POST]]))

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

(defroutes websocket-routes
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post                req)))

(def websocket-app
  (-> websocket-routes
      ring.middleware.keyword-params/wrap-keyword-params
      ring.middleware.params/wrap-params))


(defn send-msg [msg]
  (doseq [uid (:any @connected-uids)]
    (chsk-send! uid msg)))

(send-msg [:fast-push/is-fast 111])
@connected-uids


