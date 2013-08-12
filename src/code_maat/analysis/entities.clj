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

(defn by-revision
  "Sorts all entities in the dataset ds by
   their number of revisions."
  ([ds]
     (by-revision ds :desc))
  ([ds order-fn]
     (let [g ($group-by :entity ds)]
       ($order :n-revs order-fn
               (dataset [:entity :n-revs]
                        (map group->entity-with-rev-count g))))))
  