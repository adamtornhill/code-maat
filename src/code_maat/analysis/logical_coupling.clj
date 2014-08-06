;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.logical-coupling
  (:require [clojure.math.combinatorics :as combo]
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

(defn- drop-duplicates
  [entities]
  (remove (fn [[f s]] (= f s)) entities))

;; TODO: use sort-by, apply compare
(defn- drop-mirrored-modules
  [entities]
  (->
   (map (fn [[f s]]
          (if (pos? (compare f s))
            [s f]
            [f s]))
        entities)
   distinct))

(defn- as-co-changing-modules
  "Returns pairs representing the modules
   coupled in the given change set.
   Note that we keep single modules that
   aren't coupled - we need them to calculate
   the correct number of total revisions."
  [entities]
  (->
   (combo/selections entities 2)
   drop-mirrored-modules))
   
(defn- as-entities-by-revision
  "Extracts the change set per revision
   from an Incanter dataset."
  [ds]
  (->>
   ($ [:rev :entity] ds) ; minimal
   (ds/-group-by :rev)
   (map second)))

(defn- within-threshold?
  "Used to filter the results based on user options."
  [{:keys [min-revs min-shared-revs min-coupling max-coupling]}
   revs shared-revs coupling]
  {:pre [(and min-revs min-shared-revs min-coupling max-coupling)]}
  (and
   (>= revs min-revs)
   (>= shared-revs min-shared-revs)
   (>= coupling min-coupling)
   (<= (math/floor coupling) max-coupling)))

(def entities-in-rev
  (partial ds/-select-by :entity))

(defn- modules-in-one-rev
  "We receive pairs of co-changing modules in a
   revision  and return a seq of all distinct modules."
  [m]
  (->
   (flatten m)
   distinct))

(defn- module-by-revs
  "Returns a map with each module as key and
   its number of revisions as value.
   This is used when calculating the degree
   of coupling later."
  [all-co-changing] 
  (->
   (mapcat modules-in-one-rev all-co-changing)
   frequencies))

(defn co-changing-by-revision
  "Calculates a vector of all entities coupled
  in the revision represented by the dataset."
  [ds]
  (->>
   (as-entities-by-revision ds)
   (map entities-in-rev)
   (map as-co-changing-modules)))

(defn- coupling-frequencies
  [co-changing]
  "Returns a map with pairs of coupled
   modules (pairs) as keyes and their
   number of shared revisions as value."
  (->
   (apply concat co-changing)
   drop-duplicates ; remember: included to get the right total revisions
   frequencies
   vec))
   
(defn- as-logical-coupling-measure
  "This is where the result is assembled.
   We already have all the data. Now we just pass through the
   coupled modules, with their co-change frequencies, and
   transform it to a degree of coupling.
   The coupling formula is simple: the number of shared
   revisions divided by the average number of revisions for
   the two coupled modules."
  [ds within-threshold-fn?]
  (let [co-changing (co-changing-by-revision ds)
        module-revs (module-by-revs co-changing)
        coupling (coupling-frequencies co-changing)]
    (for [[[first-entity second-entity] shared-revs] coupling
          :let [average-revs (m/average (module-revs first-entity) (module-revs second-entity))
                coupling (m/as-percentage (/ shared-revs average-revs))]
          :when (within-threshold-fn? average-revs shared-revs coupling)]
      {:entity first-entity :coupled second-entity
       :degree (int coupling) :average-revs (int (math/ceil average-revs))})))

(defn by-degree
  "Calculates the degree of logical coupling. Returns a seq
   sorted in descending order (default) or an optional, custom sorting criterion.
   The calulcation is  based on the given coupling statistics.
   The coupling is calculated as a percentage value based on
   the number of shared commits between coupled entities divided
   by the average number of total commits for the coupled entities."
  ([ds options]
     (by-degree ds options :desc))
  ([ds options order-fn]
     (->>
      (partial within-threshold? options)
      (as-logical-coupling-measure ds)
      (ds/-dataset [:entity :coupled :degree :average-revs])
      ($order [:degree :average-revs] order-fn))))

