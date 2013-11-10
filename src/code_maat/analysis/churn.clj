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

(defn- sum-by-date
  "Sums the given dataset by date and churn.
   The dataset is grouped on :date. That means,
   each entry is a pair of date and changes recorded in
   that day. The changes are Incanter datasets themselves
   so we can keep using Incanter to extract data for each
   group."
  [grouped]
  (for [[date-entry changes] grouped
        :let [date (:date date-entry)
              added (total-churn :loc-added changes)
              deleted (total-churn :loc-deleted changes)]]
    [date added deleted]))

(defn absolutes-trend
  [ds options]
  (throw-on-missing-data ds)
  (->>
   (ds/-group-by :date ds)
   (sum-by-date)
   (ds/-dataset [:date :added :deleted])))
