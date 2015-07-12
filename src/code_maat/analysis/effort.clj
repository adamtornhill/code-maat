;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.effort
  (:require [code-maat.dataset.dataset :as ds]
            [incanter.core :as incanter]
            [clojure.math.numeric-tower :as m]
            [code-maat.analysis.math :as math]))

;;; The idea behind effort is to identify how much each author
;;; contributed to a module. The measure here is a bit more
;;; rough than the churn metrics. On the other hand, the metric
;;; is available for all supported VCS.
;;; I use the generated statistics as a guide when refactoring;
;;; by ranking the authors based on their amount of contribution
;;; I know who to ask when visiting a new module.
;;;
;;; The analysis in the module is based on research by
;;; Marco D’Ambros, Harald C. Gall, Michele Lanza, and Martin Pinzger.

(defn normalize-effort
  [[name effort]]
  (map (fn [[author revs total-revs]]
         [name author revs total-revs])
       effort))

(defn- sum-revs-by-author
  "Sums the given dataset by a given group and churn.
   The given dataset, grouped-ds, is grouped by the column
   given as group.
   That means, each entry is a pair of some grouping construct
   and the changes related to that construct. The changes are
   Incanter datasets themselves so we can keep using
   Incanter to extract data for each group."
  [grouped total-revs]
  (for [[group-entry changes] grouped
        :let [author (:author group-entry)
              revs (ds/-nrows changes)]]
    [author revs total-revs]))

(defn- sum-effort-by-author
  [grouped]
  (for [[entity-entry changes] grouped
        :let [entity (:entity entity-entry)
              total-revs (ds/-nrows changes)
              author-group (ds/-group-by :author changes)
              author-revs (sum-revs-by-author author-group total-revs)]]
    [entity author-revs]))

(defn as-revisions-per-author
  [ds options]
  (->>
   (ds/-group-by :entity ds)
   sum-effort-by-author
   (mapcat normalize-effort)
   (ds/-dataset [:entity :author :author-revs :total-revs])
   (ds/-order-by :entity :asc)))

(defn- contributed-revs
  [author-changes]
  (let [[_author added _total] author-changes]
    added))

(defn- pick-main-dev-by-rev
  [entity-ds]
  (let [[entity entity-changes] entity-ds
        main-dev-changes (first (sort-by contributed-revs > entity-changes))
        [author added total] main-dev-changes
        ownership (math/ratio->centi-float-precision (/ added total))]
    [entity author added total ownership]))

(defn as-main-developer-by-revisions
  "Identifies the main developers, together with their
   ownership percentage, of each module."
  [ds options]
  (->>
   (ds/-group-by :entity ds)
   sum-effort-by-author
   (map pick-main-dev-by-rev)
   (ds/-dataset [:entity :main-dev :added :total-added :ownership])
   (ds/-order-by :entity :asc)))

(defn- as-author-fractals
  [[_ ai nc]]
  (m/expt (/ ai nc) 2))

(defn- as-fractal-value
  [[name effort]]
  (let [[_1 _2 total-revs] (first effort) ; same as nc
        fv1 (reduce + (map as-author-fractals effort))
        fv (math/ratio->centi-float-precision (- 1 fv1))]
    [name fv total-revs]))

(defn as-entity-fragmentation
  "Caclulates a fractal value for each entity.
   The fractal value ranges from 0 (one author) to
   1 (many authors, unreachable value).
   The fractal value is a good complement to number of
   authors analyses since here we reduce smaller contributions
   more and get a chance to find the truly fragmented entities."
  [ds options]
  (->>
   (ds/-group-by :entity ds)
   sum-effort-by-author
   (map as-fractal-value)
   (ds/-dataset [:entity :fractal-value :total-revs])
   (ds/-order-by [:fractal-value :total-revs] :desc)))
