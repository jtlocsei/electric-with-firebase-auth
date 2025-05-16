(ns electric-starter-app.firebase-client
  (:require ["firebase/app" :refer [initializeApp]]
            ["firebase/auth" :refer [createUserWithEmailAndPassword
                                     getAuth
                                     GoogleAuthProvider
                                     onAuthStateChanged
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


;; Track the sign in state
;; https://firebase.google.com/docs/auth/web/start#set_an_authentication_state_observer_and_get_user_data
(onAuthStateChanged firebase-auth
  (fn [user]
    (if user
      (do
        (js/console.log "Auth state changed. Adding user")
        (swap! !client-db db/set-user user))
      (do
        (js/console.log "Auth state changed. Removing user")
        (swap! !client-db db/remove-user)))))
(comment
  ; User's email
  (some-> (db/get-user @!client-db) .-email)

  ; Print user object to console
  (js/console.log (db/get-user @!client-db))
  :_)


;; We can also see the current user using the currentUser property (nil if no-one logged in)
(comment
  (.-currentUser firebase-auth)
  :_)


;; Create a new user with email and password
(comment
  (-> (createUserWithEmailAndPassword firebase-auth "john@acme.com" "password123")
    (.then (fn [user-credential]
             (let [user (.-user user-credential)]
               (reset! !user user)
               (js/console.log "Signed up user:")
               (js/console.log user))))
    (.catch (fn [error]
              (js/console.log "Sign up error code:" (.-code error))
              (js/console.log "Sign up error message:" (.-message error)))))

  (js/console.log @!user)
  :_)


;; Sign in a user with email and password
(comment
  (-> (signInWithEmailAndPassword firebase-auth "john@acme.com" "password123")
    (.then (fn [user-credential]
             (let [user (.-user user-credential)]
               (reset! !user user)
               (js/console.log "Signed in user:")
               (js/console.log user))))
    (.catch (fn [error]
              (js/console.log "Sign in error code: " (.-code error))
              (js/console.log "Sign in error message" (.-message error)))))
  :_)


;; Sign out the current user (cljs)
;; https://firebase.google.com/docs/auth/web/password-auth (scroll to bottom)
(comment
  (-> (signOut firebase-auth)
    (.then (fn [] (js/console.log "Sign-out successful")))
    (.catch (fn [error]
              (js/console.log "Error when trying to sign out")
              (js/console.log error))))
  :_)


;; Sign in a user with Google
;; https://firebase.google.com/docs/auth/web/google-signin
(comment
  (-> (signInWithPopup firebase-auth google-auth-provider)
    (.then (fn [result]
             (let [user (.-user result)]
               (reset! !user user)
               (js/console.log "Signed in user:")
               (js/console.log user))))
    (.catch (fn [error]
              (js/console.log "Sign in with google error code:" (.-code error))
              (js/console.log "Sign in with google error message:" (.-message error))
              (js/console.log "Sign in with google error user's email:" (-> error .-customData .-email)))))
  :_)


;; Print ID token of current user
(comment
  (-> (.getIdToken @!user)
    (.then (fn [id-token]
             (println id-token)))
    (.catch (fn [error]
              (js/console.log "Error with getIdToken of user" error))))
  :_)