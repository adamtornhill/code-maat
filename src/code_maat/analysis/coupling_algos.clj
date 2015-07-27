;;; Copyright (C) 2014 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.coupling-algos
  (:require [clojure.math.combinatorics :as combo]
            [code-maat.dataset.dataset :as ds]
            [code-maat.analysis.math :as m]
            [clojure.math.numeric-tower :as math])
  (:use incanter.core))

;;; This module contains the shared algorithms for the
;;; different coupling measures.

(defn- drop-duplicates
  [entities]
  (remove #(= % (reverse %)) entities))

(defn- drop-mirrored-modules
  "Removed mirrored change sets such as:
    [A B] [B A] => [A B]"
  [entities]
  (->
   (map sort entities)
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
   
(defn as-entities-by-revision
  "Extracts the change set per revision
   from an Incanter dataset."
  [ds]
  (->>
   ($ [:rev :entity] ds) ; minimal
   (ds/-group-by :rev)
   (map second)))

(defn within-threshold?
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

(def modules-in-one-rev
  "We receive pairs of co-changing modules in a
   revision  and return a seq of all distinct modules."
  (comp distinct flatten))

(defn module-by-revs
  "Returns a map with each module as key and
   its number of revisions as value.
   This is used when calculating the degree
   of coupling later."
  [all-co-changing] 
  (->
   (mapcat modules-in-one-rev all-co-changing)
   frequencies))

(defn exceeds-max-changeset-size?
  [max-size change-set]
  (> (count change-set) max-size))

(defn co-changing-by-revision
  "Calculates a vector of all entities coupled
  in the revision represented by the dataset."
  [ds options]
  (->>
   (as-entities-by-revision ds)
   (map entities-in-rev)
   (remove (partial exceeds-max-changeset-size? (:max-changeset-size options)))
   (map as-co-changing-modules)))

(defn coupling-frequencies
  [co-changing]
  "Returns a map with pairs of coupled
   modules (pairs) as keyes and their
   number of shared revisions as value."
  (->
   (apply concat co-changing)
   drop-duplicates ; remember: included to get the right total revisions
   frequencies
   vec))
