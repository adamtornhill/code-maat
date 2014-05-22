;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.communication
  (:require [code-maat.dataset.dataset :as ds]
            [code-maat.analysis.effort :as effort]
            [clojure.math.combinatorics :as combo]
            [code-maat.analysis.math :as m]
            [clojure.math.numeric-tower :as math]
            [incanter.core :as incanter]))

;;; This module attempts to give some heuristics on
;;; the communication needs of a project.
;;; The idea is basedo on Conway's law - a project
;;; works best when its organizational structure is
;;; mirrored in software.
;;;
;;; The algorithm is similiar to the one used for
;;; logical coupling: calculate the number of shared
;;; commits between all permutations of authors.
;;; Based on their total averaged commits, a
;;; communication strength value is calculated.

(defn- authors-of
  [changes]
  (distinct
   (ds/-select-by :author changes)))

;;; When calculating frequencies we get all permutations,
;;; including "noise" like self-self pairs. We use that
;;; noise to carry information for us - self-self will
;;; give us a fast way to look-up the total number of
;;; commits for an author.

(defn- authorship-combos 
  [authors]
  (combo/selections authors 2))

(defn- entity-group->authorship-combos
  [[entity-entry changes]]
  (authorship-combos (authors-of changes)))

(defn- author-pairs-for-entities
  "Transforms the given dataset (grouped on entity) into
   a seq of author pairs. The frequency of each pair in
   the returned seq will specify their amount of shared
   work over the grouped entities."
  [grouped]
  (mapcat entity-group->authorship-combos grouped))

(defn- by-shared-work-frequency
  [authors-by-work]
  (frequencies authors-by-work))

(defn- commits-of
  [author freqs]
  (freqs [author author]))

(defn- strength-from
  [shared-commits average-commits]
  (int
   (m/as-percentage
    (/ shared-commits average-commits))))

(defn- with-commit-stats
  "The statistics are calculated from the raw
   data, freqs. The data contains pairs for all
   authors with their shared work frequencies.
   The statistics (i.e. total number of commits) for
   any author is retrieved by looking-up the
   value for the author paired with himself.
   That self-pairing is stripped from the final
   statistics but used here to carry information."
  [freqs]
  (for [[pair shared-commits] freqs
       :let [[me peer] pair
             my-commits (commits-of me freqs)
             peer-commits (commits-of peer freqs)
             average-commits (math/ceil
                              (m/average my-commits peer-commits))
             strength (strength-from shared-commits average-commits)]
       :when (not (= me peer))]
    [me peer shared-commits average-commits strength]))

(defn by-shared-entities
  "Caclulates the communication needs as based upon
   shared work by the authors on different entities.
   Returns a dataset containing pairs of all permutations
   of authors with a (heuristic) communication strength
   value for each pair." 
  [ds options]
  (->>
   (effort/as-revisions-per-author ds options)
   (ds/-group-by :entity)
   author-pairs-for-entities
   by-shared-work-frequency
   with-commit-stats
   (ds/-dataset [:author :peer :shared :average :strength])
   (ds/-order-by [:strength :author] :desc)))

