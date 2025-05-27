(ns electric-starter-app.firebase-server
  (:require [clojure.java.io :as io])
  (:import [com.google.auth.oauth2 GoogleCredentials]
           [com.google.firebase FirebaseApp FirebaseOptions]
           [com.google.firebase.auth AuthErrorCode FirebaseAuth FirebaseAuthException FirebaseToken]
           [java.util Map$Entry]))



;; Initialise the firebase SDK on the server.
;; Replace the path below with the path to your Firebase Admin SDK service account JSON file.
;; To get your service account credentials:
;; 1. Go to Firebase Console -> Project Settings -> Service Accounts
;; 2. Click "Generate New Private Key"
;; 3. Save the downloaded JSON file in your .secret directory
;; 4. Update the path below to match your JSON filename
;; Note: In production, use GOOGLE_APPLICATION_CREDENTIALS environment variable to specify path
;; to JSON file instead of hard-coding it.
;; SECURITY WARNING: Never commit your service account key to source control!
;; The .gitignore file is set up to prevent this, but be careful if you rename or move the file.
;; Further reading:
;; https://firebase.google.com/docs/admin/setup#initialize_the_sdk_in_non-google_environments
(defn init-firebase! ; called in dev.clj and prod.cljc
  []
  (let [path        (or (System/getenv "GOOGLE_APPLICATION_CREDENTIALS")
                      ".secret/electric-auth-firebase-adminsdk-fbsvc-a484b36f6c.json")
        stream      (io/input-stream path)
        credentials (GoogleCredentials/fromStream stream)
        options     (-> (FirebaseOptions/builder)
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
  (verify-id-token "paste id token here")
  ;=>
  ;{:verified true,
  ; :claims   {:firebase       {"identities"       {"google.com" ["105552262235837203905"], "email" ["________@gmail.com"]},
  ;                             "sign_in_provider" "google.com"},
  ;            :email          "________@gmail.com",
  ;            :aud            "electric-auth",
  ;            :sub            "B8mH7YlD3fZxTq9rWv2aLpOsRgCn",
  ;            :iss            "https://securetoken.google.com/electric-auth",
  ;            :name           "______ ________",
  ;            :exp            1747661821,
  ;            :email_verified true,
  ;            :auth_time      1747558132,
  ;            :picture        "https://lh3.googleusercontent.com/a/ACd9pQwErBxu8NkL3zYmFtXvJeA6-GtVmsLoUzRkWvXiCpqhdJ7MgZTb=s96-c",
  ;            :user_id        "B8mH7YlD3fZxTq9rWv2aLpOsRgCn",
  ;            :iat            1747658221}}

  (verify-id-token nil) ; => {:verified false, :error :token-is-nil}
  :_)


(defn ^:private when-verified*
  "Internal helper. Verifies the Firebase ID token using `verify-id-token`.
   If the token is valid, calls (f claims & args), where `claims` is the map
   returned from the decoded token.

   The Firebase UID is available under (:user_id claims).

   If the token is invalid or revoked, returns nil and does not invoke `f`.

   - `check-revoked?`: boolean, whether to check for token revocation (slower but more secure)
   - `f`: function to call with claims and any args
   - `id-token`: Firebase ID token (JWT string)
   - `args`: optional additional args passed to `f`"
  [id-token check-revoked? f & args]
  (let [{:keys [verified claims]} (verify-id-token id-token :check-revoked check-revoked?)]
    (when verified
      (apply f claims args))))


(defn when-verified
  "Verifies the Firebase ID token and, if valid, calls (f claims & args),
   where `claims` is the decoded Firebase claims map.

   The Firebase UID is available under (:user_id claims).

   If the token is invalid or expired, returns nil and does not invoke `f`.

   Does NOT check whether the token has been revoked."
  [id-token f & args]
  (apply when-verified* id-token false f args))


(defn when-verified-strict
  "Verifies the Firebase ID token and checks whether it has been revoked. It's slower
  than verify-call because it makes a call to the Firebase server to check whether the
  token has been revoked, but also more secure for the same reason. See Firebase docs
  https://firebase.google.com/docs/auth/admin/manage-sessions#detect_id_token_revocation

  If token is valid and not revoked, calls (f claims & args), where `claims` is the
  decoded Firebase claims map.

  The Firebase UID is available under (:user_id claims).

  If the token is invalid, expired, or revoked, returns nil and does not invoke `f`."
  [id-token f & args]
  (apply when-verified* id-token true f args))


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
  (revoke-refresh-tokens "B8mH7YlD3fZxTq9rWv2aLpOsRgCn")
  :_)
