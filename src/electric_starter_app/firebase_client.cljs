(ns electric-starter-app.firebase-client
  (:require ["firebase/app" :refer [initializeApp]]
            ["firebase/auth" :refer [createUserWithEmailAndPassword
                                     getAuth
                                     GoogleAuthProvider
                                     onAuthStateChanged
                                     onIdTokenChanged
                                     signInWithEmailAndPassword
                                     signInWithPopup
                                     signOut]]
            [electric-starter-app.db :as db :refer [!client-db]]))


(def firebase-config
  #js {:apiKey            "AIzaSyC2-qOKdgDLnCiw5Iq7NgX8CRr47outuGU"
       :authDomain        "electric-auth.firebaseapp.com"
       :projectId         "electric-auth"
       :storageBucket     "electric-auth.firebasestorage.app"
       :messagingSenderId "274711424672"
       :appId             "1:274711424672:web:922b3fd25bc4be6d2123c5"})


(defonce firebase-app
  (initializeApp firebase-config))


(defonce firebase-auth
  (getAuth firebase-app))


(defonce google-auth-provider
  (GoogleAuthProvider.))


;; Track the sign in state and save the user object in the client db
;; https://firebase.google.com/docs/auth/web/start#set_an_authentication_state_observer_and_get_user_data
(defonce !auth-state-change-listener-added? (atom false))
(when-not @!auth-state-change-listener-added?
  (do
    (onAuthStateChanged firebase-auth
      (fn [user]
        (if user
          (do
            (js/console.log "Auth state changed. Adding user")
            (swap! !client-db db/set-user user))
          (do
            (js/console.log "Auth state changed. Removing user")
            (swap! !client-db db/remove-user)))))
    (reset! !auth-state-change-listener-added? true)))
(comment
  ; User's email
  (some-> (db/get-user @!client-db) .-email)

  ; Print user object to console
  (js/console.log (db/get-user @!client-db))

  ;; We can also see the current user using the currentUser property (nil if no-one logged in)
  (.-currentUser firebase-auth)
  :_)


;; Keep track of the latest id token, which Firebase auto-refreshes every hour or so while logged in
;; https://firebase.google.com/docs/reference/js/v8/firebase.auth.Auth#onidtokenchanged
(defonce !token-change-listener-added? (atom false))
(when-not @!token-change-listener-added?
  (onIdTokenChanged firebase-auth
    (fn [user]
      (if user
        (do
          (-> (.getIdToken user)
            (.then (fn [id-token]
                     (swap! !client-db db/set-id-token id-token)))
            (.catch (fn [error]
                      (js/console.log "Error with getIDToken" error))))
          (js/console.log "ID token changed. User is logged in"))
        (do
          (swap! !client-db db/remove-id-token)
          (js/console.log "ID token changed. User logged out")))))
  (reset! !token-change-listener-added? true))


;; Sign in a user with Google
;; https://firebase.google.com/docs/auth/web/google-signin
(defn sign-in-with-google
  []
  (-> (signInWithPopup firebase-auth google-auth-provider)
    (.then (fn [result]
             (let [user (.-user result)]
               (js/console.log "Signed in user:")
               (js/console.log user))))
    (.catch (fn [error]
              (js/console.log "Sign in with google error code:" (.-code error))
              (js/console.log "Sign in with google error message:" (.-message error))
              (js/console.log "Sign in with google error user's email:" (-> error .-customData .-email))))))
(comment
  (sign-in-with-google)
  :_)


;; Sign out the current user (cljs)
;; https://firebase.google.com/docs/auth/web/password-auth (scroll to bottom)
(defn sign-out
  []
  (-> (signOut firebase-auth)
    (.then (fn [] (js/console.log "Sign-out successful")))
    (.catch (fn [error]
              (js/console.log "Error when trying to sign out")
              (js/console.log error)))))
(comment
  (sign-out)
  :_)


;; Create a new user with email and password
(comment
  (-> (createUserWithEmailAndPassword firebase-auth "john@acme.com" "password123")
    (.then (fn [user-credential]
             (let [user (.-user user-credential)]
               (js/console.log "Signed up user:")
               (js/console.log user))))
    (.catch (fn [error]
              (js/console.log "Sign up error code:" (.-code error))
              (js/console.log "Sign up error message:" (.-message error)))))
  :_)


;; Sign in a user with email and password
(comment
  (-> (signInWithEmailAndPassword firebase-auth "john@acme.com" "password123")
    (.then (fn [user-credential]
             (let [user (.-user user-credential)]
               (js/console.log "Signed in user:")
               (js/console.log user))))
    (.catch (fn [error]
              (js/console.log "Sign in error code: " (.-code error))
              (js/console.log "Sign in error message" (.-message error)))))
  :_)


;; Print ID token of current user
(comment
  (-> (.getIdToken (db/get-user @!client-db))
    (.then (fn [id-token]
             (println id-token)))
    (.catch (fn [error]
              (js/console.log "Error with getIdToken of user" error))))
  :_)


(defn refresh-id-token []
  (when-let [user (.-currentUser firebase-auth)]
    (-> (.getIdToken user true) ; force refresh true
      (.then (fn [id-token]
               (swap! !client-db db/set-id-token id-token)
               (js/console.log "Refreshed ID token")))
      (.catch (fn [error]
                (js/console.warn "ID token refresh failed. " error))))))


;; Refresh ID token when tab becomes visible again
(defonce !tab-visibility-listener-added? (atom false))
(when-not @!tab-visibility-listener-added?
  (.addEventListener js/document "visibilitychange"
    (fn []
      (when (= (.-visibilityState js/document) "visible")
        (refresh-id-token))))
  (reset! !tab-visibility-listener-added? true))


;; Refresh ID token every 5 minutes as fallback
(defonce !token-refresh-timer-added? (atom false))
(when-not @!token-refresh-timer-added?
  (js/setInterval
    refresh-id-token
    (* 1000 60 5))
  (reset! !token-refresh-timer-added? true))