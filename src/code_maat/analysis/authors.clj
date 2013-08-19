(ns code-maat.analysis.authors
  (:require [code-maat.dataset.dataset :as ds]
            [code-maat.analysis.entities :as entities]))

;;; This module contains analysis methods related to the authors of the VCS commits.
;;; Research shows that these metrics (e.g. number of authors of a module) are
;;; related to the number of quality problems that module exhibits.
;;;
;;; Format:
;;; All analysis expect an Incanter dataset with the following columns:
;;; :author :entity :rev

(defn of-module [m ds]
  (ds/-dataset [:author]
               (set
                (ds/-select-by
                 :author
                 (ds/-where {:entity m} ds)))))

(defn all
  "Returns a set with the name of all authors."
  [ds]
  (set (ds/-select-by :author ds)))

(defn entity-with-author-count
  "Calculates the number of different authors for the given module, m.
   Returns a tuple of [entity-name number-of-distinct-authors]."
  [m ds]
  [m (ds/-nrows (of-module m ds))])

(defn- authors-of-entity
  [entity-group]
  (->>
   entity-group
   (ds/-select-by :author)
   set
   count))

(defn- group->entity-with-author-count
  [[entity-group entity-changes] ds]
  (let [entity (:entity entity-group)]
    [entity
     (authors-of-entity entity-changes)
     (entities/revisions-of entity ds)]))

(defn by-count
  "Groups all entities by there total number of authors.
   By default, the entities are sorted in descending order.
   You can provide an extra, optional argument specifying
   a custom criterion.
   Returns a dataset with the columns :entity :n-authors."
  ([ds options]
     (by-count ds options :desc))
  ([ds options order-fn]
     (let [g (ds/-group-by :entity ds)
           by-rev (entities/as-dataset-by-revision ds)]
       (->>
        g
        (map #(group->entity-with-author-count % by-rev))
         (ds/-dataset [:entity :n-authors :n-revs])
         (ds/-order-by :n-authors order-fn)))))