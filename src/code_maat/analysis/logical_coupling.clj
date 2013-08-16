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

(defn- entities-in-rev
  [rev-ds]
  (ds/-select-by :entity rev-ds))

(defn- make-entity<->coupled-pair
  [[entity coupled]]
  {:entity entity :coupled coupled})

(defn- make-entity<->coupled-pair-NEW
  [[entity coupled]]
  [entity coupled])

(defn in-same-revision
  "Calculates a vector of all entities coupled in
   the given dataset for one revision.
   The returned vector contains maps of :entity and :coupled."
  [rev-ds]
  (->>
   rev-ds
   entities-in-rev
   as-coupling-permutations
   (map make-entity<->coupled-pair)))

(defn- shared-commits
  [coupled entity-coupling-ds]
  (nrow
   ($where {:coupled coupled} entity-coupling-ds)))

(defn- average [x y] (/ (+ x y) 2))

(defn- as-percentage [v] (* v 100))

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

;;; New

;;; New implementation:
;;; This is all we need:
;;;{"Entity" {:revs n :coupled {"C1" 2  "C2" 1}}
;;; We can quickly look-up total revs for all entities and
;;; their dependencies and shared commits.

;;; CURRENT: changing coupled from vec to map!

(defn- make-entity-stats [] {:revs 0 :coupled {}})

(defn- ensure-entity
  [entity stats]
  {:post  [(% entity)]}
  (update-in stats [entity]
             (fnil identity
                   (make-entity-stats))))

(defn- inc-revs
  [entity entity-stats]
  {:pre  [(entity-stats entity)]}
  (update-in entity-stats [entity]
             #(update-in % [:revs] inc)))

(defn update-entity-rev-in
  [stats entity]
  (inc-revs entity (ensure-entity entity stats)))

;;; TODO: ensure seems to be a macro candidate!
(defn- ensure-coupling
  [entity coupling-stats]
  {:post [(% entity)]}
  (update-in coupling-stats [entity]
             (fnil identity 0)))

(defn- inc-coupling
  [coupled coupled-stats]
  {:pre  [(coupled-stats coupled)]}
  (update-in coupled-stats [coupled] inc))

(defn- add-coupling
  [entity coupled entity-spec]
  (update-in entity-spec
             [:coupled]
             #(inc-coupling coupled
                            (ensure-coupling coupled %))))

(defn update-coupling-in
  [stats {:keys [entity coupled]}]
  {:pre  [(stats entity)]}
  (update-in stats
             [entity]
             #(add-coupling entity coupled %)))

(defn- as-dependents-in-one-rev
  [stats changes-in-rev]
  (let [entities (entities-in-rev changes-in-rev) ; TODO: abstract better! macro?
        stats-with-updated-revs (reduce update-entity-rev-in
                                        stats
                                        entities)]
    (reduce update-coupling-in
            stats-with-updated-revs
            (in-same-revision changes-in-rev))))
            

(defn as-dependent-entities
  [changes-grouped-by-rev]
  (reduce as-dependents-in-one-rev
          {}
          (map second
               changes-grouped-by-rev)))

(defn calc-dependencies
  [ds]
  (->
   ds
   grouped-by-rev
   as-dependent-entities))

(defn- n-entity-revs
  [entity dependencies]
  {:pre [(dependencies entity)]}
  (:revs (dependencies entity)))

(defn as-logical-coupling
  [all-dependencies [entity {:keys [revs coupled]}]]
   "This is where the actual action is - we receive a
   map for each entity with its total number of revisions and
   another map of its coupled entities. Based on that information
   we calculate the degree of coupling between the entity and
   each of its coupled counterparts.
   Future: consider weighting the total number of revisions into
   the calculation to avoiding skewed data."
   (for [[coupled shared-revs] coupled
        :let [coupled-revs (n-entity-revs coupled all-dependencies)
              average-revs (average revs coupled-revs)
              coupling (as-percentage (/ shared-revs average-revs))]]
     {:entity entity :coupled coupled :degree coupling}))

(defn as-logical-coupling-of-all
  [all-dependencies]
  (map #(as-logical-coupling all-dependencies %)
       all-dependencies))

(defn by-degree1
  "Calculates the degree of logical coupling. Returns a seq
   sorted in descending order (default) or an optional, custom sorting criterion.
   The calulcation is  based on the given coupling statistics.
   The coupling is calculated as a percentage value based on
   the number of shared commits between coupled entities divided
   by the average number of total commits for the coupled entities."
  ([ds] (by-degree1 ds :desc))
  ([ds order-fn]
     (->>
      ds
      calc-dependencies
      as-logical-coupling-of-all
      flatten
      to-dataset
      ($order :degree order-fn))))

;;; End new

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

(defn- as-logical-coupling1
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
        ($map as-logical-coupling1 [:entity :coupled :shared-revs :average-revs])
        to-dataset
       ($order :degree order-fn)))))