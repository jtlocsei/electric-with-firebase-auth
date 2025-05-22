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


(defn token->claims-map
  "Extracts the claims from a FirebaseToken as a plain Clojure map
   with keywordized keys.

   The result is a true Clojure map — not a Java-backed map — to avoid
   issues where some operations (e.g. `walk/keywordize-keys`) fail due
   to Guava's immutable map internals.

   Example return:
   {:user_id \"abc123\", :email \"foo@example.com\", :admin true, ...}

   Note: This manually walks the Java Map entries because `(into {})`
   on `.getClaims` may return a Guava map that does not behave like
   a native Clojure map."
  [^FirebaseToken token]
  (into {}
    (for [^Map$Entry entry (.entrySet (.getClaims token))]
      [(keyword (.getKey entry)) (.getValue entry)])))


(defn verify-id-token
  "Verifies the Firebase ID token and optionally checks for revocation.

   On success, returns:
     {:verified true, :claims {...}}

   On failure, returns one of:
     {:verified false, :reason :revoked}
     {:verified false, :reason :invalid}
     {:verified false, :reason :nil-token}"
  [id-token & {:keys [check-revoked] :or {check-revoked true}}]
  ;; Docs https://firebase.google.com/docs/auth/admin/verify-id-tokens
  (if id-token
    (try
      (let [auth   (FirebaseAuth/getInstance)
            token  (.verifyIdToken auth id-token check-revoked)
            claims (token->claims-map token)]
        {:verified true, :claims claims})
      (catch FirebaseAuthException e
        (let [error-code (.getAuthErrorCode e)]
          {:verified false
           :reason (if (= error-code AuthErrorCode/REVOKED_ID_TOKEN)
                     :revoked
                     :invalid)})))
    {:verified false :reason :nil-token}))


(comment
  (verify-id-token "eyJhbGciOiJSUzI1NiIsImtpZCI6IjY3ZDhjZWU0ZTYwYmYwMzYxNmM1ODg4NTJiMjA5MTZkNjRjMzRmYmEiLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiVG9iaWFzIExvY3NlaSIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BQ2c4b2NMRW5Tenc1ZkpnN3Z5RzRkYVJtUGYzLUZyRnZzSGlxT2hQZUxjTWlIdGJoRTRJaENJYj1zOTYtYyIsImlzcyI6Imh0dHBzOi8vc2VjdXJldG9rZW4uZ29vZ2xlLmNvbS9lbGVjdHJpYy1hdXRoIiwiYXVkIjoiZWxlY3RyaWMtYXV0aCIsImF1dGhfdGltZSI6MTc0NzcwNDg4MywidXNlcl9pZCI6ImVTN0k1bnl6WGJZdmJJeFFqZTdubVRSVUdLdjEiLCJzdWIiOiJlUzdJNW55elhiWXZiSXhRamU3bm1UUlVHS3YxIiwiaWF0IjoxNzQ3ODk0OTkwLCJleHAiOjE3NDc4OTg1OTAsImVtYWlsIjoianRsb2NzZWlAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZ29vZ2xlLmNvbSI6WyIxMDU1NTIyNjIyMzU4MzcyMDM5MDUiXSwiZW1haWwiOlsianRsb2NzZWlAZ21haWwuY29tIl19LCJzaWduX2luX3Byb3ZpZGVyIjoiZ29vZ2xlLmNvbSJ9fQ.lHik3Oo-y197xuVaMH5Fe4QQgdEnnGkaYijxH6Be9RCW3sYW8pbNp9QX4k33otr0EahTVgPReIvr5sEa8sqJsX1iwNCRmH_Lz-WKlkMJxd0XX32QaVU3J0wcSqdOjhGpVCc23D2irRSqkpWu8TsDJGbItXht4i0-M1iyw4fQceOOXDsKYoUf2nlGMcCNEj1NUVCIu2zoMQXLRtBWsow9uOiliBz8RLGaNLLx2l4WYK3hbWenTUk_rv-HnL-aB7fcQfbuYNOhPH7frFLP1sOyVMBcR5xjO6FyDBD593ruuLWORJ2Lc1C52Ux3qkY2z52rXNmDAlx6i1nQdnn5KsKobA")
  ;=>
  ;{:verified true,
  ; :claims   {:firebase       {"identities"       {"google.com" ["105552262235837203905"], "email" ["________@gmail.com"]},
  ;                             "sign_in_provider" "google.com"},
  ;            :email          "________@gmail.com",
  ;            :aud            "electric-auth",
  ;            :sub            "eS7I5nyzXbYvbIxQje7nmTRUGKv1",
  ;            :iss            "https://securetoken.google.com/electric-auth",
  ;            :name           "______ ________",
  ;            :exp            1747661821,
  ;            :email_verified true,
  ;            :auth_time      1747558132,
  ;            :picture        "https://lh3.googleusercontent.com/a/ACg8ocLEnSzw5fJg7vyG4daRmPf3-FrFvsHiqOhPeLcMiHtbhE4IhCIb=s96-c",
  ;            :user_id        "eS7I5nyzXbYvbIxQje7nmTRUGKv1",
  ;            :iat            1747658221}}

  (verify-id-token nil) ; => {:verified false, :error :token-is-nil}
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
   returned from the decoded token.

   The Firebase UID is available under (:user_id claims).

   If the token is invalid or revoked, returns nil and does not invoke `f`.

   - `id-token`: Firebase ID token (JWT string)
   - `check-revoked?`: boolean, whether to reject revoked tokens
   - `f`: function to call with claims and any args
   - `args`: optional additional args passed to `f`"
  [id-token check-revoked? f & args]
  (let [{:keys [verified claims]} (verify-id-token id-token :check-revoked check-revoked?)]
    (when verified
      (apply f claims args))))


(defn with-auth
  "Verifies the Firebase ID token and, if verified, calls (f claims & args),
   where `claims` is the decoded Firebase claims map.

   The Firebase UID is available under (:user_id claims).

   If the token is invalid or revoked, returns nil and does not invoke `f`.

   Does NOT check whether the token has been revoked."
  [id-token f & args]
  (apply with-auth* id-token false f args))


(defn with-auth-check-revoked
  "Verifies the Firebase ID token and checks whether it has been revoked.
   If verified and not revoked, calls (f claims & args), where `claims` is the
   decoded Firebase claims map.

   The Firebase UID is available under (:user_id claims).

   If the token is invalid or revoked, returns nil and does not invoke `f`."
  [id-token f & args]
  (apply with-auth* id-token true f args))