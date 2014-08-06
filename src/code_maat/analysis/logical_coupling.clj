;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.logical-coupling
  (:require [clojure.math.combinatorics :as combo]
            [code-maat.analysis.entities :as entities]
            [code-maat.dataset.dataset :as ds]
            [code-maat.analysis.math :as m]
            [clojure.math.numeric-tower :as math])
  (:use incanter.core))

;;; This module calculates the logical coupling of all modules.
;;;
;;; Logical coupling refers to modules that tend to change together.
;;; It's information that's recorded in our version-control systems (VCS).
;;;
;;; Input: all analysis expect an Incanter dataset with (at least) the following columns:
;;; :entity :rev
;;;
;;; Oputput: the analysis returns an Incanter dataset with the following columns:
;;; :entity :coupled :degree :average-revs

(defn- as-coupling-permutations
  [entities]
  (remove (fn [[f s]] (= f s))
          (combo/selections entities 2)))

(defn- drop-mirrored-modules
  [entities]
  (->
   (map (fn [[f s]]
          (if (pos? (compare f s))
            [s f]
            [f s]))
        entities)
   distinct))

(defn- entities-in-rev
  [rev-ds]
  (ds/-select-by :entity rev-ds))

(defn- make-entity<->coupled-pair
  [[entity coupled]]
  {:entity entity :coupled coupled})

(defn in-same-revision
  "Calculates a vector of all entities coupled in
   the given dataset for one revision.
   The returned vector contains maps of :entity and :coupled."
  [rev-ds]
  (->>
   rev-ds
   entities-in-rev
   as-coupling-permutations
   drop-mirrored-modules
   (map make-entity<->coupled-pair)))

(defn- grouped-by-rev
  [flat-data]
  (->>
   ($ [:rev :entity] flat-data) ; minimal
   (ds/-group-by :rev)))

(defn- make-entity-stats [] {:revs 0 :coupled {}})

(defmacro ensure-exists
  "Ensures that the entity exists in the given stat(istics).
   When not, the entity is added and bound to the value
   returned from evaluating the default form."
  [entity stats default]
  `(update-in ~stats [~entity]
              (fnil identity
                    ~default)))
(defn- inc-revs
  [entity entity-stats]
  {:pre  [(entity-stats entity)]}
  (update-in entity-stats [entity]
             #(update-in % [:revs] inc)))

(defn update-entity-rev-in
  [stats entity]
  (inc-revs entity
            (ensure-exists entity
                           stats
                           (make-entity-stats))))

(defn- inc-coupling
  [coupled coupled-stats]
  {:pre  [(coupled-stats coupled)]}
  (update-in coupled-stats [coupled] inc))

(defn- add-coupling
  [entity coupled entity-spec]
  (update-in entity-spec
             [:coupled]
             #(inc-coupling coupled
                            (ensure-exists coupled % 0))))

(defn update-coupling-in
  [stats {:keys [entity coupled]}]
  {:pre  [(stats entity)]}
  (update-in stats
             [entity]
             #(add-coupling entity coupled %)))

(defn- as-updated-revisions
  [entities stats]
  (reduce update-entity-rev-in
          stats
          entities))

(defn- as-dependents-in-one-rev
  [stats changes-in-rev]
  (let [entities (entities-in-rev changes-in-rev)
        stats-with-updated-revs (as-updated-revisions entities stats)]
    (reduce update-coupling-in
            stats-with-updated-revs
            (in-same-revision changes-in-rev))))

(defn- changes-in-rev [g]
  "Extracts the change set from an Incanter
   dataset grouped by revision."
  (map second g))

(defn as-dependent-entities
  [changes-grouped-by-rev]
  (->>
   changes-grouped-by-rev
   (changes-in-rev)
   (reduce as-dependents-in-one-rev
          {})))

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

(defn- within-threshold?
  [{:keys [min-revs min-shared-revs min-coupling max-coupling]}
   revs shared-revs coupling]
  {:pre [(and min-revs min-shared-revs min-coupling max-coupling)]}
  (and
   (>= revs min-revs)
   (>= shared-revs min-shared-revs)
   (>= coupling min-coupling)
   (<= (math/floor coupling) max-coupling)))

(defn as-logical-coupling
  [all-dependencies within-threshold-fn? [entity {:keys [revs coupled]}]]
   "This is where the actual action is - we receive a
   map for each entity with its total number of revisions and
   another map of its coupled entities. Based on that information
   we calculate the degree of coupling between the entity and
   each of its coupled counterparts.
   Future: consider weighting the total number of revisions into
   the calculation to avoiding skewed data."
   (for [[coupled shared-revs] coupled
         :let [coupled-revs (n-entity-revs coupled all-dependencies)
               average-revs (m/average revs coupled-revs)
               coupling (m/as-percentage (/ shared-revs average-revs))]
         :when (within-threshold-fn? average-revs shared-revs coupling)]
     {:entity entity :coupled coupled
      :degree (int coupling) :average-revs (math/ceil average-revs)}))

(defn as-logical-coupling-of-all
  [thresholds all-dependencies]
  (mapcat (partial as-logical-coupling
                   all-dependencies
                   (partial within-threshold? thresholds))
          all-dependencies))

(defn by-degree
  "Calculates the degree of logical coupling. Returns a seq
   sorted in descending order (default) or an optional, custom sorting criterion.
   The calulcation is  based on the given coupling statistics.
   The coupling is calculated as a percentage value based on
   the number of shared commits between coupled entities divided
   by the average number of total commits for the coupled entities."
  ([ds options] (by-degree ds options :desc))
  ([ds options order-fn]
     (->>
      ds
      calc-dependencies
      (as-logical-coupling-of-all options)
      (ds/-dataset [:entity :coupled :degree :average-revs])
      ($order [:degree :average-revs] order-fn))))
