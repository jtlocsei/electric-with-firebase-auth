(ns electric-starter-app.firebase-stuff
  (:require ["firebase/app" :refer [initializeApp]]
            ["firebase/auth" :refer [getAuth
                                     createUserWithEmailAndPassword
                                     signInWithEmailAndPassword]]))


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


;const auth = getAuth();
;createUserWithEmailAndPassword(auth, email, password)
;  .then((userCredential) => {
;    // Signed up
;    const user = userCredential.user;
;    // ...
;  })
;  .catch((error) => {
;    const errorCode = error.code;
;    const errorMessage = error.message;
;    // ..
;  });

(defonce !user (atom nil))

;; Try creating a new user
(comment
  (-> (createUserWithEmailAndPassword firebase-auth "john@acme.com" "password123")
    (.then (fn [user-credential]
             (let [user (.-user user-credential)]
               (reset! !user user)
               (js/console.log "Signed up user:")
               (js/console.log user))))
    (.catch (fn [error]
              (println "Sign up error code:" (.-code error))
              (println "Sign up error message:" (.-message error)))))

  (js/console.log @!user)
  :_)


