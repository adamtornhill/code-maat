;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.churn
  (:require [code-maat.dataset.dataset :as ds]
            [incanter.core :as incanter]
            [code-maat.analysis.math :as math]))

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
(defn- total-count
  [ds]
  (ds/-nrows ds))

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
              count (total-count changes)
              added (total-churn :loc-added changes)
              deleted (total-churn :loc-deleted changes)]]
    [grouping added deleted count]))

(defn- sum-by-author-contrib
  [grouped]
  (for [[entity-entry changes] grouped
        :let [entity (:entity entity-entry)
              author-group (ds/-group-by :author changes)
              author-contrib (sum-by-group :author author-group)]]
    [entity author-contrib]))

(defn- normalize-contrib
  "Each entity is associated with its contributing authors.
   An author may be included multiple times. We need to
   join that info and splice one entity into multiple rows
   to make up an Incanter dataset.
   Example on input:
    [Entity [[ta 20 2] [at 2 0]]]
  Should result in:
    [Entity ta 20 2]
    [Entity at  2 0]"
  [[name contribs]]
  (map (fn [[a add del]] [name a add del]) contribs))

(defn- churn-by
  [group ds options]
  (throw-on-missing-data ds)
  (->>
   (ds/-group-by group ds)
   (sum-by-group group)
   (ds/-dataset [group :added :deleted :commits])))

(defn absolutes-trend
  "Calculates the absolute code churn measures per date.
   Returns an Incanter dataset with the number of lines
   added and deleted each day (note that only dates wich
   involved commits are considered)."
  [commits options]
  (->>
  (churn-by :date commits options)
  (ds/-order-by [:date :added :deleted] :asc)))

(defn by-author
  "Sums the total churn for each contributing author."
  [commits options]
  (->>
   (churn-by :author commits options)
   (ds/-order-by [:author :added] :asc)))

(defn by-entity
  "Returns the absolute churn of each entity.
   The entities are sorted at churn rate in
   descending order based on the lines added
   metric. The idea is that entities
   with higher churn rate (even absolute) are
   more likely to contain post-release defects, where
   the number of lines added is a better predictor
   than lines deleted."
  [ds options]
  (->>
   (churn-by :entity ds options)
   (ds/-order-by :added :desc)))

(defn as-ownership
  "Returns a table specifying the ownership of
   each module. Ownership is defined as the
   amount of churn contributed by each author
   to each entity."
  [ds options]
  (throw-on-missing-data ds)
  (->>
   (ds/-group-by :entity ds)
   sum-by-author-contrib
   (mapcat normalize-contrib)
   (ds/-dataset [:entity :author :added :deleted])
   (ds/-order-by :entity :asc)))

;;
;;; Algorithms to identify main developers from churn
;;;

(defn- added-lines
  [[_author added _deleted]]
  added)

(defn- removed-lines
  [[_author _added deleted]]
  deleted)

(def developer first)

(defn- as-ownership-ratio
  [own total]
  (->>
   (max total 1) ; some entities don't have any added lines (just removed)
   (/ own)
   math/ratio->centi-float-precision))

(defn- pick-main-developer
  "Picks the developer that contributed most lines of
   code (sure, a rough measure).
   Returns [Entity Developer Added Total-Added]
   Example on input:
    [Entity [[ta 20 2] [at 2 0]]]
  Should result in:
    [Entity ta 20 2]"
  ([author-contrib]
     (pick-main-developer added-lines author-contrib))
  ([metric-fn [name contribs]]
     (let [total-contrib (reduce + (map metric-fn contribs))
           main-dev (first (reverse (sort-by metric-fn contribs)))
           main-dev-contrib (metric-fn main-dev)
           ownership-ratio (as-ownership-ratio main-dev-contrib total-contrib)]
       [name (developer main-dev) main-dev-contrib total-contrib ownership-ratio])))

(defn- grouped-by-author-contrib
  [ds]
  (throw-on-missing-data ds)
  (->
   (ds/-group-by :entity ds)
   sum-by-author-contrib))


(defn by-main-developer
  "Identifies the main developer of each entity.
   The main developer is the one who has contributed
   most lines of code (default case).
   NOTE: see the alternative algorithm below!"
  [ds options]
  (->>
   (grouped-by-author-contrib ds)
   (map pick-main-developer)
   (ds/-dataset [:entity :main-dev :added :total-added :ownership])
   (ds/-order-by :entity :asc)))

(defn by-refactoring-main-developer
  "Identifies the main developer of each entity.
   The main developer in this alternative calculation
   is the developer that has _removed_ most lines.
   The idea/speculation is that when you remove code,
   you make a more active design choice than what can be
   expected from addition alone. We speak refactoring here."
  [ds options]
  (->>
   (grouped-by-author-contrib ds)
   (map (partial pick-main-developer removed-lines))
   (ds/-dataset [:entity :main-dev :removed :total-removed :ownership])
   (ds/-order-by :entity :asc)))
