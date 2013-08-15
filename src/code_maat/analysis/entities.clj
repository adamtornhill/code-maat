(ns code-maat.analysis.entities
  (:require [code-maat.dataset.dataset :as ds]
            [incanter.core :as incanter]))

(defn all [ds]
  (set (ds/-select-by :entity ds)))

(defn- group->entity-with-rev-count
  [[entity-group changes]]
  [(:entity entity-group)
   (count
    (ds/-select-by :rev changes))])

(defn as-dataset-by-revision
  [ds]
  (let [g (ds/-group-by :entity ds)]
    (ds/-dataset [:entity :n-revs]
                 (map group->entity-with-rev-count g))))

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
  ([ds]
     (by-revision ds :desc))
  ([ds order-fn]
     (ds/-order-by :n-revs order-fn
             (as-dataset-by-revision ds))))
  