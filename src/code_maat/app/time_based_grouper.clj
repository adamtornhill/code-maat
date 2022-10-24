;;; Copyright (C) 2014 Adam Tornhill
;;;

(ns code-maat.app.time-based-grouper
  (:require [clj-time.core :as t]
            [clj-time.format :as tf]
            [clj-time.periodic :as time-period]
            [clj-time.core :as tc]
            [medley.core :as m]))

;;; Sometimes we'd like to use a different temporal window than
;;; the commit. For example, when multiple teams are involved
;;; changes may have to be done in multiple commits as well.
;;; Further, some organizations use a workflow that involves
;;; many small commits.
;;; To remove these biases we use this module to re-group all
;;; changes according to a given time window before analysis.
;;;
;;; Grouping commits by time involves a sliding window over the
;;; original commits. This means that logically, the same physical commit
;;; can be counted multiple times since it overlaps with several slides
;;; of the window. This works well for change coupling but not hotspots.
;;; Hence, the validation ensures it's a supported analysis before
;;; applying the filter.

(defn- string->date
  [s]
  (tf/parse (tf/formatters :year-month-day) s))

(defn date->string
  [d]
  (tf/unparse (tf/formatters :year-month-day) d))

(defn- date-of
  [cs]
  (some-> cs first :date string->date))

(defn- daily-dates-between
  "Create a range of DateTime objects where each date represens one day."
  [start end]
  (let [feeding-range (time-period/periodic-seq start (tc/days 1))
        end-condition-date (tc/plus end (tc/days 1))
        full-range? (fn [current-date] (t/before? current-date end-condition-date))]
    (take-while full-range? feeding-range)))

(defn- pad-commits-to-complete-time-series
  "There are probably many days which don't have any commits.
   This functions pads up those days with empty commit sets. That way, we can
   partition over the sequence and easily create the sliding window commit set."
  [commits]
  (let [commits-ascending (sort-by :date commits)
        first-commit-date (date-of commits-ascending)
        last-commit-date (date-of (reverse commits-ascending))
        commits-on-non-active-days []]
    (reduce (fn [acc date-in-range]
              (let [as-date (date->string date-in-range)
                    commits-on-day (get acc as-date commits-on-non-active-days)]
                (assoc acc as-date commits-on-day)))
            (group-by :date commits)
            (daily-dates-between first-commit-date last-commit-date))))

(defn- drop-date-key
  "We used group-by to get commits by date. Now, drop the key so that
  only the commits remain."
  [grouped-commits]
  (map second grouped-commits))

(defn- remove-empty-windows
  "Not all dates have commit activity."
  [commits-within-sliding-windows]
  (remove (fn [cs]
            (every? empty? cs))
          commits-within-sliding-windows))

(defn- adjust-revision-to
  "The edge case is that the same file should only be included once, so
   let's filter out duplicates."
  [new-rev cs]
  (->> cs
       (map (fn [c]
              (assoc c :rev new-rev)))
       (m/distinct-by :entity)))

(defn- combine-commits-to-logical-changesets
  [commits-within-sliding-windows]
  (mapcat (fn [commits-in-window]
            (let [cs (reduce (partial into) commits-in-window)
                  latest-day (->> cs (sort-by :date) reverse first :date)]
              (adjust-revision-to latest-day cs)))
          commits-within-sliding-windows))

(defn- combine-sliding-commits
  "After partitioning commits according to the sliding window, we
   need to deliver a flat sequence where each commit group in the window
   represents a logical commitset."
  [commits-within-sliding-windows]
  (->> commits-within-sliding-windows
       remove-empty-windows
       combine-commits-to-logical-changesets))

(defn- partition-commits-into-sliding-periods-of
  [time-period padded-cs]
  (->> padded-cs
       (sort-by first)
       drop-date-key
       (partition time-period 1)))

(defn- commits->sliding-window-seq
  [time-period cs]
  (->> cs
       pad-commits-to-complete-time-series
       (partition-commits-into-sliding-periods-of time-period)
       combine-sliding-commits))

(defn- validated-time-period-from
  [{:keys [temporal-period] :as _options}]
  (if (re-matches #"\d+" temporal-period)
    (int (Double/parseDouble temporal-period))
    (throw (IllegalArgumentException.
             (str "Invalid time-period: the given value '" temporal-period "' is not an integer.")))))

(defn by-time-period
  "Alright, this is a hack: we just set the commit ID to
   the current date. That makes the rest of the analyses treat
   our faked grouping as beloning to the same change set."
  [cs options]
  (let [time-period (validated-time-period-from options)]
    (if (seq cs)
      (commits->sliding-window-seq time-period cs)
      cs)))
  
