(ns code-maat.analysis.authors
  (:require [code-maat.analysis.workarounds :as workarounds])
  (:use incanter.core))

;;; This module contains analysis methods related to the authors of the VCS commits.
;;; Research shows that these metrics (e.g. number of authors of a module) are
;;; related to the number of quality problems that module exhibits.
;;;
;;; Format:
;;; All analysis expect an Incanter dataset with the following columns:
;;; :author :entity :rev

(defn of-module [m ds]
  (set
   (workarounds/fix-single-return-value-bug
    ($ :author ($where {:entity m} ds)))))

(defn all
  "Returns a set with the name of all authors."
  [ds]
  (set ($ :author ds)))

(defn entity-with-author-count
  "Calculates the number of different authors for the given module, m.
   Returns a tuple of [entity-name number-of-distinct-authors]."
  [m ds]
  [m (count (of-module m ds))])

(defn- group->entity-with-author-count
  [[entity-group changes]]
  [(:entity entity-group)
   (count
    (set
     (workarounds/fix-single-return-value-bug
      ($ :author changes))))])

(defn by-count
  "Groups all entities by there total number of authors.
   By default, the entities are sorted in descending order.
   You can provide an extra, optional argument specifying
   a custom criterion.
   Returns a dataset with the columns :entity :n-authors."
  ([ds]
     (by-count ds :desc))
  ([ds order-fn]
     (let [g ($group-by :entity ds)]
       ($order :n-authors order-fn
               (dataset [:entity :n-authors]
                        (map group->entity-with-author-count g))))))