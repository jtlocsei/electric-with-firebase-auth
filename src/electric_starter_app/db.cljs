(ns electric-starter-app.db)


(def default-db
  {})


(defonce !client-db (atom default-db))


(comment
  (reset! !client-db default-db)
  @!client-db
  :_)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Getters and Setters
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn set-user
  [db firebase-user-object]
  (assoc db :user firebase-user-object))
(comment
  (set-user default-db {:about "This is a fake user"})
  :_)


(defn remove-user
  [db]
  (dissoc db :user))
(comment
  (let [updated-db (set-user default-db {:about "This is a fake user"})]
    (remove-user updated-db))
  :_)


(defn get-user
  [db]
  (:user db))
(comment
  (get-user default-db)
  (get-user (set-user default-db {:about "fake user"}))
  :_)


(defn set-id-token
  [db firebase-id-token]
  (assoc db :id-token firebase-id-token))
(comment
  (set-id-token default-db 1234)
  :_)


(defn remove-id-token
  [db]
  (dissoc db :id-token))
(comment
  (remove-id-token (set-id-token default-db 1234))
  :_)


(defn get-id-token
  [db]
  (:id-token db))
(comment
  (get-id-token (set-id-token default-db 1234))
  :_)