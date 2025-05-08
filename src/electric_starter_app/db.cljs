(ns electric-starter-app.db)


(defn default-db
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