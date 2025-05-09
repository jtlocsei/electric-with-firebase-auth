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