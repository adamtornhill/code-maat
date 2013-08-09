(ns code-maat.analysis.authors
  (:require [code-maat.analysis.entities :as entities])
  (:use incanter.core))

;;; Todo: fix single author case!
(defn authors-of-module [m ds]
  (set ($ :author ($where {:entity m} ds))))
;;;User=> (authors-of-module "A" svnd)
;;;#{"apt" "jt"}

(defn all [ds]
  (set ($ :author ds)))
;;;user=> (all-authors svnd)
;;;#{"xy" "apt" "jt"}

(defn entity-with-author-count [ds m]
  [m (count (authors-of-module m ds))])

;;; API
;;; The intended use is to ask for modules by-author-count.
;;; If we want to analyze which authors, we typically use
;;; authors-of-module.

;;; TODO: remove this one - not necessary!
(defn by-authors [ds]
  "Deduces the authors of each module in the dataset (ds).
   Returns a seq of tuples where each tuple:
   [entity seq-of-its-authors]."
  (for [e (entities/all ds)
        :let [authors [e (authors-of-module e ds)]]]
    authors))
;;;user=> (by-authors svnd)
;;;(["A" #{"apt" "jt"}] ["B" #{\a \p \t}])

(defn by-author-count
  ([ds]
     (by-author-count ds :desc))
  ([ds order-fn]
     ($order :n-authors order-fn 
             (dataset [:entity :n-authors]
                      (map (partial entity-with-author-count ds)
                           (entities/all ds))))))