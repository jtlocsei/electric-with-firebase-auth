(ns electric-starter-app.firebase-server
  (:require [clojure.java.io :as io])
  (:import [com.google.auth.oauth2 GoogleCredentials]
           [com.google.firebase FirebaseApp FirebaseOptions]
           [com.google.firebase.auth AuthErrorCode FirebaseAuth FirebaseAuthException FirebaseToken]))



;; Initialise the firebase SDK on the server
(defonce _firebase-initialisation ; use defonce to make sure code only runs once
  (let [stream      (io/input-stream ".secret/electric-auth-firebase-adminsdk-fbsvc-a484b36f6c.json")
        credentials (GoogleCredentials/fromStream stream)
        options     (-> (FirebaseOptions/builder) ; line 134
                      (.setCredentials credentials)
                      (.build))]
    (FirebaseApp/initializeApp options)))


(defn verify-id-token
  "Verifies the Firebase ID token and checks for revocation.
  Returns a map like:
    {:status :ok, :uid uid}
    {:status :revoked}
    {:status :invalid}"
  [id-token & {:keys [check-revoked] :or {check-revoked true}}]
  ;; Docs https://firebase.google.com/docs/auth/admin/verify-id-tokens
  (if id-token
    (try
      (let [auth          (FirebaseAuth/getInstance)
            token         (.verifyIdToken auth id-token check-revoked)
            uid           (.getUid ^FirebaseToken token)]
        {:status :ok, :uid uid})
      (catch FirebaseAuthException e
        (let [error-code (.getAuthErrorCode e)]
          (cond
            (= error-code AuthErrorCode/REVOKED_ID_TOKEN)
            {:status :revoked}

            :else
            {:status :invalid}))))
    {:status :token-is-nil}))


(comment
  (verify-id-token "eyJhbGciOiJSUzI1NiIsImtpZCI6IjU5MWYxNWRlZTg0OTUzNjZjOTgyZTA1MTMzYmNhOGYyNDg5ZWFjNzIiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vZWxlY3RyaWMtYXV0aCIsImF1ZCI6ImVsZWN0cmljLWF1dGgiLCJhdXRoX3RpbWUiOjE3NDY5MTc4NDYsInVzZXJfaWQiOiJEaTVsa21OVzBiYXFyUmFWVFNoVDZ0dWNUWncyIiwic3ViIjoiRGk1bGttTlcwYmFxclJhVlRTaFQ2dHVjVFp3MiIsImlhdCI6MTc0NjkxNzg0NiwiZXhwIjoxNzQ2OTIxNDQ2LCJlbWFpbCI6ImpvaG5AYWNtZS5jb20iLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZW1haWwiOlsiam9obkBhY21lLmNvbSJdfSwic2lnbl9pbl9wcm92aWRlciI6InBhc3N3b3JkIn19.yoMDNvhUfAAwI5da151R_p3ylt1FUmorp9i9x3JLyMvQiQfyAZ8N8v-YdgNhY_112pUV6eo2jrJw3KPAiD-0m4h0N1mweFBHYbdhr0pALK_o4UxJfkT0B-M6fCHZ1mLp9QAbn7BmOGZobjIgA8XG3GQ1Rstbi2xaIyHKqbLMfPnGaucP9abhqkIA2TsL8phJbfwjIQPdvBVJdYYYkVUjKGrOxlRRCGEGHw0eaDYyzvG1Mt1xCKw-9GcwVqF78CjQ-mXeuBHX2rKyirv6GkNKBgVavx9y_J7HlthbHIQT9T7Xz-2o38b49amW992kpZBo9maBmSBQipOBV5amTJh9Tw") ; => {:status :invalid}
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