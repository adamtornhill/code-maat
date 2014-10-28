;;; Copyright (C) 2014 Adam Tornhill
;;;

(ns code-maat.app.time-based-grouper)

;;; Sometimes we'd like to use a different temporal window than
;;; the commit. For example, when multiple teams are involved
;;; changes may have to be done in multiple commits as well.
;;; Further, some organizations use a workflow that involves
;;; many small commits.
;;; To remove these biases we use this module to re-group all
;;; changes according to a given time window before analysis.
;;;
;;; LIMITATION: At the moment we only support grouping commits that
;;; occour within the same day. This is because I could implement
;;; that aggregation easily. I plan to extend Code Maat with
;;; support for arbitrary temporal windows.

(defn- date-as-commit-id
  [commit]
  (let [date (:date commit)]
    (update-in commit [:rev] (fn [_old] date))))

(defn- throw-on-invalid
  [time-period]
  (when (not (= "1" time-period)) ; Let's support more in the future...
    (throw
     (IllegalArgumentException.
      (str "Invalid time-period: the current version only supports one (1) day")))))

(defn run
  "Alright, this is a hack: we just set the commit ID to
   the current date. That makes the rest of the analyses treat
   our faked grouping as beloning to the same change set."
  ([raw-data]
     (run raw-data "1"))
  ([raw-data time-period]
     (throw-on-invalid time-period)
     (map date-as-commit-id raw-data)))
  
