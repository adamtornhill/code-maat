;;; Copyright (C) 2014-2015 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.sum-of-coupling
  (:require [code-maat.dataset.dataset :as ds]
            [code-maat.analysis.coupling-algos :as c])
  (:use incanter.core))

;;; This module calculates the sum of the temporal coupling for each module.
;;;
;;; The metric gives the number of shared transactions for a module.
;;; This gives you a priority list of the modules that are most
;;; frequently changed together with others.
;;;
;;; The analysis returns a dataset with the following columns:
;;;   :entity :soc
;;; where
;;;   :entity => the name of the module
;;;   :soc    => the sum of the coupling

(defn- entities-by-revision
  [ds]
  (->>
   (c/as-entities-by-revision ds)
   (map c/entities-in-rev)))

(defn- counted-entities
  [entities-in-rev]
  (let [n-couples (- (count entities-in-rev) 1)]
    (map (fn [e] [e n-couples]) entities-in-rev)))

(defn- entities-with-coupling-count-by-rev
  [ds]
  (->> ds
   entities-by-revision
   (mapcat counted-entities)))

(defn as-soc
  "Calculates a Sum of Coupling for each entity in 
   the dataset that passes the threshold for minimum 
   number of revisions."
  [ds {:keys [min-revs]}]
  (->> ds
       entities-with-coupling-count-by-rev
       (reduce (fn [acc [e n]]
                 (update-in acc [e] (fnil + 0) n))
               {})
       (into [])
       (filter (fn [[e n]] 
                 (> n min-revs)))))

(defn by-degree
  "Calculates the sum of coupling. Returns a seq
   sorted in descending order (default) or an optional,
   custom sorting criterion."
  ([ds options]
     (by-degree ds options :desc))
  ([ds options order-fn]
     (->>
      (as-soc ds options)
      (ds/-dataset [:entity :soc])
      ($order [:soc :entity] order-fn))))
