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


(e/defn LogoutButton
  []
  (dom/button (dom/text "Logout")
    (let [[spend err] (e/Token (dom/On "click" identity nil))]
      (when spend
        (case (fbc/sign-out) (spend))))))


(e/defn SignInWithGoogleButton
  []
  (dom/button (dom/text "Sign in with Google")
    (let [[spend err] (e/Token (dom/On "click" identity nil))]
      (when spend
        (case (fbc/sign-in-with-google) (spend))))))



(e/defn Main [ring-request]
  (e/client
    (binding [dom/node js/document.body
              e/http-request (e/server ring-request)]
      ; mandatory wrapper div https://github.com/hyperfiddle/electric/issues/74
      (dom/div (dom/props {:style {:display "contents"}})
        (dom/h1 (dom/text "Demo of Firebase Auth With Electric"))
        (let [<client-db (e/watch !client-db)
              id-token (db/get-id-token <client-db)
              user-email (some-> (db/get-user <client-db) .-email)]
          (if (= :ok (e/server (:status (fbs/verify-id-token id-token))))
            (dom/div
              ;; TODO add demo of performing secure action on the server, by passing id-token and verifying it on the server
              (dom/h2 (dom/text "You are logged in"))
              (dom/p (dom/text "You are logged in as " user-email))
              (LogoutButton))
            (dom/div
              (dom/h2 (dom/text "You are not logged in"))
              (SignInWithGoogleButton)))
          (dom/h3 (dom/text "Debug info"))
          (dom/div (dom/text "User: " user-email))
          (dom/div (dom/text "ID Token: " id-token))
          (dom/div (dom/text "Status: " (e/server (fbs/verify-id-token id-token)))))))))