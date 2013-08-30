;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.entities
  (:require [code-maat.dataset.dataset :as ds]
            [incanter.core :as incanter]))

(defn all [ds]
  (distinct (ds/-select-by :entity ds)))

(defn- group->entity-with-rev-count
  [[entity-group changes]]
  [(:entity entity-group)
   (count
    (ds/-select-by :rev changes))])

(defn all-revisions
  [ds]
  (distinct (ds/-select-by :rev ds)))

(defn as-dataset-by-revision
  [ds]
  (->>
   ds
   (ds/-group-by :entity)
   (map group->entity-with-rev-count)
   (ds/-dataset [:entity :n-revs])))

(defn revisions-of
  "Returns the total number of revisions for the given
   entity in the dataset ds, which must be grouped
   by revisions."
  [entity by-revision-ds]
  (incanter/$ ; here we actually want to return a single value!
   :n-revs
   (ds/-where {:entity entity} by-revision-ds)))

(defn by-revision
  "Sorts all entities in the dataset ds by
   their number of revisions."
  ([ds options]
     (by-revision ds options :desc))
  ([ds options order-fn]
     (ds/-order-by :n-revs order-fn
             (as-dataset-by-revision ds))))
  