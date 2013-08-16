(ns code-maat.analysis.logical-coupling
  (:require [clojure.math.combinatorics :as combo]
            [code-maat.analysis.entities :as entities]
            [code-maat.dataset.dataset :as ds])
  (:use incanter.core))

;;; This module calculates the logical coupling of all modules.
;;;
;;; Logical coupling refers to modules that tend to change together.
;;; It's information that's recorded in our version-control systems (VCS).
;;;
;;; Format:
;;; All analysis expect an Incanter dataset with (at least) the following columns:
;;; ::entity :rev

(defn- as-coupling-permutations
  [entities]
  (remove (fn [[f s]] (= f s))
          (combo/selections entities 2)))

(defn in-same-revision
  "Calculates a vector of all entities coupled in
   the given dataset for one revision.
   The returned vector contains maps of :entity and :coupled."
  [rev-ds]
  (->>
   rev-ds
   (ds/-select-by :entity)
   as-coupling-permutations
   (map (fn [[f s]] {:entity f :coupled s}))))

(defn- shared-commits
  [coupled entity-coupling-ds]
  (nrow
   ($where {:coupled coupled} entity-coupling-ds)))

(defn- average
  [x y]
  (/ (+ x y) 2))

(defn commit-stats
  [entity coupled entities-by-rev entity-coupling-ds]
  (let [entity-revs (entities/revisions-of entity entities-by-rev)
        coupled-revs (entities/revisions-of coupled entities-by-rev)]
    {:average-revs (average entity-revs coupled-revs)
     :shared-revs (shared-commits coupled entity-coupling-ds)}))

(defn- coupling-statistics-for-entity
  "Calculates the coupling statistics for the given entity.
   Returns a map with the following keys:
   :entity :coupled :shared-revs :average-revs"
  [[e eds] entities-by-rev]
  (let [entity (:entity e)
        coupled (set (ds/-select-by :coupled eds))
        commit-stats-fn #(commit-stats entity % entities-by-rev eds)]
    (for [c coupled
          :let [{:keys [average-revs shared-revs]} (commit-stats-fn c)
                stats {:entity entity
                       :coupled c
                       :shared-revs shared-revs
                       :average-revs average-revs}]]
      stats)))

(defn- coupling-statistics
  "Applies the coupling statistics to each entity in the
   given dataset."
  [all-coupled entity-by-rev]
  (->>
   all-coupled
   (ds/-group-by :entity)
   (map #(coupling-statistics-for-entity % entity-by-rev))
   flatten))

(defn- coupling-by-entity
  "Returns a dataset with the columns :entity :coupled
   aggregated over all revisions in the given ds."
  [by-rev-ds]
  (->>
   by-rev-ds
   (map (fn [[_ r-changes]] (in-same-revision r-changes)))
   flatten
   to-dataset))

(defn- grouped-by-rev
  [ds]
  (->>
   ds
   ($group-by :rev)))

(defn coupled-entities-with-rev-stats
  "Returns a seq with the columns
   :entity :coupled :shared-revs :average-revs
  This information forms the basis for the coupling calculations."
  [ds]
  (let [g (grouped-by-rev ds)
        by-rev (entities/as-dataset-by-revision ds)
        by-entity-coupling (coupling-by-entity g)]
    (coupling-statistics by-entity-coupling by-rev)))

(defn coupled-entities-with-rev-stats-as-ds
  [ds]
  (->
   ds
   coupled-entities-with-rev-stats
  to-dataset))

(defn- as-percentage [v] (* v 100))

(defn- as-logical-coupling
  [entity coupled shared-revs average-revs]
  "Future: consider weighting the total number of revisions into
   the calculation to avoiding skewed data."
  (let [coupling (as-percentage (/ shared-revs average-revs))]
    {:entity entity :coupled coupled :degree coupling}))

(defn by-degree
  "Calculates the degree of logical coupling. Returns a seq
   sorted in descending order (default) or an optional, custom sorting criterion.
   The calulcation is  based on the given coupling statistics.
   The coupling is calculated as a percentage value based on
   the number of shared commits between coupled entities divided
   by the average number of total commits for the coupled entities."
  ([ds]
     (by-degree ds :desc))
  ([ds order-fn]
     (let [coupled-with-rev (coupled-entities-with-rev-stats-as-ds ds)]
       (->>
        coupled-with-rev
        ($map as-logical-coupling [:entity :coupled :shared-revs :average-revs])
        to-dataset
       ($order :degree order-fn)))))