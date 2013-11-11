;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.churn
  (:require [code-maat.dataset.dataset :as ds]
            [incanter.core :as incanter]
            [code-maat.analysis.entities :as entities]))

;;; This module contains functions for calculating churn metrics.
;;; Code churn is related to the quality of modules; the higher
;;; the churn, the more post-release defects.
;;; Further, inspecting the churn trend lets us spot certain
;;; organization-oriented patterns. For example, we may spot
;;; integration bottlenecks as spikes just before the end of
;;; one iteration.

(defn- throw-on-missing-data
  [ds]
  (let [columns (set (incanter/col-names ds))]
    (when (or (not (columns :loc-added))
              (not (columns :loc-deleted)))
      (throw
       (IllegalArgumentException.
        (str "churn analysis: the given VCS data doesn't contain modification metrics. "
             "Check the code-maat docs for supported VCS and correct log format."))))))

(defn- as-int
  "Binaries are given as a dash.
   Ensure these are replaced by zeros."
  [v]
  (Integer/parseInt
   (if (= "-" v) "0" v)))

(defn- total-churn
  [selector ds]
  (reduce +
          (map as-int (ds/-select-by selector ds))))

(defn- sum-by-group
  "Sums the given dataset by a given group and churn.
   The given dataset, grouped-ds, is grouped by the column
   given as group.
   That means, each entry is a pair of some grouping construct
   and the changes related to that construct. The changes are
   Incanter datasets themselves so we can keep using
   Incanter to extract data for each group."
  [group grouped]
  (for [[group-entry changes] grouped
        :let [grouping (group group-entry)
              added (total-churn :loc-added changes)
              deleted (total-churn :loc-deleted changes)]]
    [grouping added deleted]))

(defn- churn-by
  [group ds options]
  (throw-on-missing-data ds)
  (->>
   (ds/-group-by group ds)
   (sum-by-group group)
   (ds/-dataset [group :added :deleted])))

(defn absolutes-trend
  "Calculates the absolute code churn measures per date.
   Returns an Incanter dataset with the number of lines
   added and deleted each day (note that only dates wich
   involved commits are considered)."
  [ds options]
  (churn-by :date ds options))

(defn by-author
  "Sums the total churn for each contributing author."
  [ds options]
  (churn-by :author ds options))
