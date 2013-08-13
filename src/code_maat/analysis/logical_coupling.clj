(ns code-maat.analysis.logical-coupling
  (:require [clojure.math.combinatorics :as combo])
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
  "Extracts the columns of interest to the coupling analysis."
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
  
;;; TODO: filter out unused columns into a new dataset (drop author).
(defn as-coupling-spec
  [[revision changes]]
  changes) ; changes is a dataset -> permutations! Another group-by entity?

(defn coupled-entities
  [ds]
  (let [g ($group-by :rev (as-entity-with-revision ds))]
    (dataset [:entity :coupled :shared-revs :total-revs]
             (map as-coupling-spec g)))) 
