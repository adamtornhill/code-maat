(ns code-maat.analysis.entities
  (:require [code-maat.analysis.workarounds :as workarounds])
  (:use incanter.core))

(defn all [ds]
  (set ($ :entity ds)))

(defn- group->entity-with-rev-count
  [[entity-group changes]]
  [(:entity entity-group)
   (count
    (workarounds/fix-single-return-value-bug
     ($ :rev changes)))])

(defn as-dataset-by-revision
  [ds]
  (let [g ($group-by :entity ds)]
    (dataset [:entity :n-revs]
             (map group->entity-with-rev-count g))))

(defn revisions-of
  "Returns the total number of revisions for the given
   entity in the dataset ds, which must be grouped
   by revisions."
  [entity by-revision-ds]
  ($ :n-revs
     ($where {:entity entity} by-revision-ds)))

(defn by-revision
  "Sorts all entities in the dataset ds by
   their number of revisions."
  ([ds]
     (by-revision ds :desc))
  ([ds order-fn]
     ($order :n-revs order-fn
             (as-dataset-by-revision ds))))
  