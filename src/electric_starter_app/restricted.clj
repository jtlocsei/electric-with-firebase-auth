(ns electric-starter-app.restricted
  (:require [electric-starter-app.firebase-server :as fbs :refer [verify-id-token]]
            [clojure.java.shell :as shell]))

;; Atom storing personal notes per user ID: {user-id -> string}
(defonce !user-notes
  (atom {}))


(defn get-note
  "Returns the saved personal note for the given user, or a default message if none exists.

   `claims` is the Firebase claims map. Expects `:user_id` key."
  [{:keys [user_id] :as claims}]
  (get @!user-notes user_id "No note yet."))


(defn set-note!
  "Updates the saved note for the given user to `new-text`.

   `claims` is the Firebase claims map. Expects `:user_id` key."
  [{:keys [user_id] :as claims} new-text]
  (swap! !user-notes assoc user_id new-text))



