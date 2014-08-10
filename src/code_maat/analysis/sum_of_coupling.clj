;;; Copyright (C) 2014 Adam Tornhill
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

(defn- soc-of
  "Returns the sum of coupling for the given entity.
    all-couplings is a seq of [couple frequency] where
    couplig => [A B]."
  [entity all-couplings]
  (->>
   (filter #(some #{entity} (first %)) all-couplings)
   (map second)
   (reduce +)))

(defn as-sum-of-coupling-measure
  [ds options]
  (let [co-changing (c/co-changing-by-revision ds options)
        module-revs (c/module-by-revs co-changing)
        coupled (c/coupling-frequencies co-changing)
        modules (keys module-revs)]
    (for [m modules
          :let [soc (soc-of m coupled)]]
      [m soc])))

(defn by-degree
  "Calculates the sum of coupling. Returns a seq
   sorted in descending order (default) or an optional,
   custom sorting criterion."
  ([ds options]
     (by-degree ds options :desc))
  ([ds options order-fn]
     (->>
      (as-sum-of-coupling-measure ds options)
      (ds/-dataset [:entity :soc])
      ($order [:soc :entity] order-fn))))
