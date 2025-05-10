(ns electric-starter-app.firebase-stuff
  #?(:cljs (:require ["firebase/app" :refer [initializeApp]]
                     ["firebase/auth" :refer [createUserWithEmailAndPassword
                                              getAuth
                                              GoogleAuthProvider
                                              onAuthStateChanged
                                              signInWithEmailAndPassword
                                              signInWithPopup
                                              signOut]]
                     [electric-starter-app.db :as db :refer [!client-db]])
     :clj (:require [clojure.java.io :as io]))
  #?(:clj (:import [com.google.auth.oauth2 GoogleCredentials]
                   [com.google.firebase FirebaseApp FirebaseOptions]
                   [com.google.firebase.auth FirebaseAuth FirebaseToken])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Client
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
    (def firebase-config
      #js {:apiKey            "AIzaSyC2-qOKdgDLnCiw5Iq7NgX8CRr47outuGU"
           :authDomain        "electric-auth.firebaseapp.com"
           :projectId         "electric-auth"
           :storageBucket     "electric-auth.firebasestorage.app"
           :messagingSenderId "274711424672"
           :appId             "1:274711424672:web:922b3fd25bc4be6d2123c5"}))


#?(:cljs
   (defonce firebase-app
     (initializeApp firebase-config)))


#?(:cljs
    (defonce firebase-auth
      (getAuth firebase-app)))


#?(:cljs
    (defonce !user (atom nil)))


;; Track the sign in state
;; https://firebase.google.com/docs/auth/web/start#set_an_authentication_state_observer_and_get_user_data
#?(:cljs
    (onAuthStateChanged firebase-auth
      (fn [user]
        (if user
          (swap! !client-db db/set-user user)
          (swap! !client-db db/remove-user)))))
(comment
  ; User's email
  (some-> (db/get-user @!client-db) .-email)

  ; Print user object to console
  (js/console.log (db/get-user @!client-db))
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


#?(:cljs
    (defonce google-auth-provider (GoogleAuthProvider.)))


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




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Server
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; https://firebase.google.com/docs/auth/admin/verify-id-tokens
;; TODO Learn how to check on server whether user logged in.

;; Initialise the firebase SDK on the server
#?(:clj
   (defonce firebase-app ; never actually use this, but use defonce to make sure it only runs once
     (let [stream      (io/input-stream ".secret/electric-auth-firebase-adminsdk-fbsvc-a484b36f6c.json")
           credentials (GoogleCredentials/fromStream stream)
           options     (-> (FirebaseOptions/builder) ; line 134
                         (.setCredentials credentials)
                         (.build))]
       (FirebaseApp/initializeApp options))))


#?(:clj
   (defn verify-id-token->uid
     "Verify id token and return uid"
     [id-token]
     (-> (FirebaseAuth/getInstance)
       (.verifyIdToken id-token)
       (.getUid))))
(comment
  (let [id-token "eyJhbGciOiJSUzI1NiIsImtpZCI6IjU5MWYxNWRlZTg0OTUzNjZjOTgyZTA1MTMzYmNhOGYyNDg5ZWFjNzIiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vZWxlY3RyaWMtYXV0aCIsImF1ZCI6ImVsZWN0cmljLWF1dGgiLCJhdXRoX3RpbWUiOjE3NDY5MTc4NDYsInVzZXJfaWQiOiJEaTVsa21OVzBiYXFyUmFWVFNoVDZ0dWNUWncyIiwic3ViIjoiRGk1bGttTlcwYmFxclJhVlRTaFQ2dHVjVFp3MiIsImlhdCI6MTc0NjkxNzg0NiwiZXhwIjoxNzQ2OTIxNDQ2LCJlbWFpbCI6ImpvaG5AYWNtZS5jb20iLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZW1haWwiOlsiam9obkBhY21lLmNvbSJdfSwic2lnbl9pbl9wcm92aWRlciI6InBhc3N3b3JkIn19.yoMDNvhUfAAwI5da151R_p3ylt1FUmorp9i9x3JLyMvQiQfyAZ8N8v-YdgNhY_112pUV6eo2jrJw3KPAiD-0m4h0N1mweFBHYbdhr0pALK_o4UxJfkT0B-M6fCHZ1mLp9QAbn7BmOGZobjIgA8XG3GQ1Rstbi2xaIyHKqbLMfPnGaucP9abhqkIA2TsL8phJbfwjIQPdvBVJdYYYkVUjKGrOxlRRCGEGHw0eaDYyzvG1Mt1xCKw-9GcwVqF78CjQ-mXeuBHX2rKyirv6GkNKBgVavx9y_J7HlthbHIQT9T7Xz-2o38b49amW992kpZBo9maBmSBQipOBV5amTJh9Tw"]
    (-> (FirebaseAuth/getInstance)
      (.verifyIdToken id-token)
      (.getUid)))
  :_)



