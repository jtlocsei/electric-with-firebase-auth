(ns electric-starter-app.main
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [electric-tutorial.chat-monitor :refer [ChatMonitor]])
  #?(:cljs
     (:require [electric-starter-app.db :as db :refer [!client-db]]
               [electric-starter-app.firebase-client :as fbc])
     :clj
     (:require [electric-starter-app.firebase-server :as fbs])))


(e/defn TopSecretStuff
  []
  (dom/div (dom/text "This is a top secret message. You shouldn't see it unless you're logged in")))


(e/defn Main [ring-request]
  (e/client
    (binding [dom/node js/document.body
              e/http-request (e/server ring-request)]
      ; mandatory wrapper div https://github.com/hyperfiddle/electric/issues/74
      (dom/div (dom/props {:style {:display "contents"}})
        #_(ChatMonitor)
        (let [<client-db (e/watch !client-db)
              id-token (db/get-id-token <client-db)]
          (dom/div (dom/text "User: " (some-> (db/get-user <client-db) .-email)))
          (dom/div (dom/text "ID Token: " id-token))
          (dom/div (dom/text "Status: " (e/server (fbs/verify-id-token id-token))))
          (if (= :ok (e/server (:status (fbs/verify-id-token id-token))))
            (dom/div (dom/text "User verified"))
            (dom/div (dom/text "User NOT verified"))))))))