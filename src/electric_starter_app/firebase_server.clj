(ns electric-starter-app.firebase-server
  (:require [clojure.java.io :as io])
  (:import [com.google.auth.oauth2 GoogleCredentials]
           [com.google.firebase FirebaseApp FirebaseOptions]
           [com.google.firebase.auth AuthErrorCode FirebaseAuth FirebaseAuthException FirebaseToken]
           [java.util Map$Entry]))



;; Initialise the firebase SDK on the server
(defonce _firebase-initialisation ; use defonce to make sure code only runs once
  (let [stream      (io/input-stream ".secret/electric-auth-firebase-adminsdk-fbsvc-a484b36f6c.json")
        credentials (GoogleCredentials/fromStream stream)
        options     (-> (FirebaseOptions/builder) ; line 134
                      (.setCredentials credentials)
                      (.build))]
    (FirebaseApp/initializeApp options)))


(defn token->claims-map [^FirebaseToken token]
  ; Uses Java magic. Plain (into {}...) returned something that looked like a map but that you couldn't
  ; keywordize the keys of.
  (into {}
    (for [^Map$Entry entry (.entrySet (.getClaims token))]
      [(keyword (.getKey entry)) (.getValue entry)])))


(defn verify-id-token
  "Verifies the Firebase ID token and checks for revocation.
  Returns a map like:
    {:status :ok, :claims {:user_id \"ak2j5lha3sd3fl\" ...}}
    {:status :revoked}
    {:status :invalid}
    {:status :token-is-nil}"
  [id-token & {:keys [check-revoked] :or {check-revoked true}}]
  ;; Docs https://firebase.google.com/docs/auth/admin/verify-id-tokens
  (if id-token
    (try
      (let [auth   (FirebaseAuth/getInstance)
            token  (.verifyIdToken auth id-token check-revoked)
            claims (token->claims-map token)]
        {:status :ok, :claims claims})
      (catch FirebaseAuthException e
        (let [error-code (.getAuthErrorCode e)]
          (cond
            (= error-code AuthErrorCode/REVOKED_ID_TOKEN)
            {:status :revoked}

            :else
            {:status :invalid}))))
    {:status :token-is-nil}))


(comment
  (verify-id-token "paste-token-here")
  ;=>
  ;{:status :ok,
  ; :claims {:firebase {"identities" {"google.com" ["105552262235837203905"], "email" ["________@gmail.com"]},
  ;                     "sign_in_provider" "google.com"},
  ;          :email "________@gmail.com",
  ;          :aud "electric-auth",
  ;          :sub "eS7I5nyzXbYvbIxQje7nmTRUGKv1",
  ;          :iss "https://securetoken.google.com/electric-auth",
  ;          :name "______ ________",
  ;          :exp 1747661821,
  ;          :email_verified true,
  ;          :auth_time 1747558132,
  ;          :picture "https://lh3.googleusercontent.com/a/ACg8ocLEnSzw5fJg7vyG4daRmPf3-FrFvsHiqOhPeLcMiHtbhE4IhCIb=s96-c",
  ;          :user_id "eS7I5nyzXbYvbIxQje7nmTRUGKv1",
  ;          :iat 1747658221}}

  (verify-id-token nil) ; => {:status :token-is-nil}
  :_)


(defn revoke-refresh-tokens
  "Revokes a user's refresh tokens and returns the revocation timestamp in seconds."
  [uid]
  (let [auth (FirebaseAuth/getInstance)]
    (.revokeRefreshTokens auth uid)
    (let [user (.getUser auth uid)
          revocation-ms (.getTokensValidAfterTimestamp user)
          revocation-s  (quot revocation-ms 1000)]
      {:uid uid
       :revoked-at revocation-s})))
(comment
  (revoke-refresh-tokens "eS7I5nyzXbYvbIxQje7nmTRUGKv1")
  :_)


(defn ^:private with-auth*
  "Internal helper. Verifies the Firebase ID token using `verify-id-token`.
   If the token is valid, calls (f claims & args), where `claims` is the map
   returned from the decoded token. If the token is invalid or revoked,
   returns nil and does not invoke `f`.

   - `id-token`: Firebase ID token (JWT string)
   - `check-revoked?`: boolean, whether to reject revoked tokens
   - `f`: function to call with claims and any args
   - `args`: optional additional args passed to `f`"
  [id-token check-revoked? f & args]
  (let [{:keys [status claims]} (verify-id-token id-token :check-revoked check-revoked?)]
    (when (= status :ok)
      (apply f claims args))))


(defn with-auth
  "Verifies the Firebase ID token and, if valid, calls (f claims & args),
   where `claims` is the decoded Firebase claims map.
   Does NOT check whether the token has been revoked."
  [id-token f & args]
  (apply with-auth* id-token false f args))


(defn with-auth-check-revoked
  "Verifies the Firebase ID token and checks whether it has been revoked.
   If valid and not revoked, calls (f claims & args), where `claims` is the
   decoded Firebase claims map."
  [id-token f & args]
  (apply with-auth* id-token true f args))