(ns electric-starter-app.main
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [electric-tutorial.chat-monitor :refer [ChatMonitor]])
  #?(:cljs
     (:require [goog.string.StringBuffer]
               [electric-starter-app.db :as db :refer [!client-db]]
               [electric-starter-app.firebase-client :as fbc]
               [cljs.pprint :refer [pprint]])
     :clj
     (:require [electric-starter-app.firebase-server :as fbs]
               [electric-starter-app.restricted :as restricted]
               [clojure.pprint :refer [pprint]])))


#?(:cljs
    (defn pprint-str [x]
      (let [sb (goog.string.StringBuffer.)]
        (binding [*print-newline* true
                  *print-fn* (fn [s] (.append sb s))]
          (pprint x))
        (str sb))))


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


(e/defn DebugInfo
  [client-db]
  (e/client)
  (let [id-token (db/get-id-token client-db)
        user-email (some-> (db/get-user client-db) .-email)
        all-user-notes (e/server (e/watch restricted/!user-notes))]
    (dom/h3 (dom/text "Debug info"))
    (dom/p (dom/text "User: " user-email))
    (dom/p (dom/text "ID Token: " id-token))
    (dom/p (dom/text "UID: " (some-> (db/get-user client-db) .-uid)))
    (dom/p (dom/text "Token Verification:\n"))
    (dom/pre (dom/code (dom/text (pprint-str (e/server (fbs/verify-id-token id-token))))))))



(e/defn LoggedIn
  [client-db]
  (let [user-email (some-> (db/get-user client-db) .-email)]
    ;; TODO add demo of performing secure action on the server, by passing id-token and verifying it on the server
    (dom/h2 (dom/text "You are logged in"))
    (dom/p (dom/text "You are logged in as " user-email))
    (LogoutButton)))


;; TODO add display of user info from server
;; TODO add button to LoggedIn to do server side action
;(dom/button
;  (let [[spend _err] (e/Token (dom/On "click" identity nil))]
;    (dom/props {:disabled (boolean spend)})
;    (when spend
;      (spend (e/server (transact! my-tx))))))


(e/defn LoggedOut
  [_client-db]
  (dom/h2 (dom/text "You are not logged in"))
  (SignInWithGoogleButton))


(e/defn Main [ring-request]
  (e/client
    (binding [dom/node js/document.body
              e/http-request (e/server ring-request)]
      ; mandatory wrapper div https://github.com/hyperfiddle/electric/issues/74
      (dom/div (dom/props {:style {:display "contents"}})
        (dom/h1 (dom/text "Demo of Firebase Auth With Electric"))
        (let [client-db  (e/watch !client-db)
              id-token   (db/get-id-token client-db)]
          (if (e/server (:verified (fbs/verify-id-token id-token)))
            (LoggedIn client-db)
            (LoggedOut client-db))
          (DebugInfo client-db))))))
