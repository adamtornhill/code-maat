(ns code-maat.analysis.logical-coupling
  (:require [clojure.math.combinatorics :as combo]
            [code-maat.analysis.entities :as entities])
  (:use incanter.core))

;;; This module calculates the logical coupling of all modules.
;;;
;;; Logical coupling refers to modules that tend to change together.
;;; It's information that's recorded in our version-control systems (VCS).
;;;
;;; Format:
;;; All analysis expect an Incanter dataset with the following columns:
;;; ::entity :rev

(defn- as-entity-with-revision
  "Extracts the columns of interest to the coupling analysis.
   The intent is to shrink the dataset received in the API to
   the minimum amount of needed data."
  [ds]
  ($ [:entity :rev] ds))

(defn- as-coupling-permutations
  [entities]
  (remove (fn [[f s]] (= f s))
          (combo/selections entities 2)))

(defn in-same-revision
  "Calculates a vector of all entities coupled in
   the given dataset for one revision.
   The returned vector contains maps of :entity and :coupled."
  [rev-ds]
  (let [entities-changed ($ :entity rev-ds)]
    (map (fn [[f s]] {:entity f :coupled s})
         (as-coupling-permutations entities-changed))))

(defn- shared-commits
  [coupled entity-coupling-ds]
  (count
   ($where {:coupled coupled} entity-coupling-ds)))

(defn- average [x y] (/ (+ x y) 2))

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
        coupled (set ($ :coupled eds))
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
  (let [cg ($group-by :entity all-coupled)]
    (flatten
     (map #(coupling-statistics-for-entity % entity-by-rev) cg))))

(defn- coupling-by-entity
  "Returns a dataset with the columns :entity :coupled
   aggregated over all revisions in the given ds."
  [by-rev-ds]
  (to-dataset
   (flatten
    (map (fn [[_ r-changes]]
           (in-same-revision r-changes))
         by-rev-ds))))

(defn coupled-entities
  "Returns a seq with the columns
   :entity :coupled :shared-revs :average-revs.
  This information forms the basis for the coupling calculations."
  [ds]
  (let [g ($group-by :rev (as-entity-with-revision ds))
        by-rev (entities/as-dataset-by-revision ds)
        by-entity-coupling (coupling-by-entity g)]
    (coupling-statistics by-entity-coupling by-rev)))

;;; TODO next: group-by entity and calculate the coupling!
