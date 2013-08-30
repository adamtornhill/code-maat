;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.summary
  (:require [code-maat.dataset.dataset :as ds]
            [code-maat.analysis.authors :as authors]
            [code-maat.analysis.entities :as entities]))

;;; This module implements a summary analysis of a given change set.
;;; The intent is to provide an overview of the data under analysis.

(defn calculate-summary
  [ds]
  "Calculates a summary for the data we'll analyze.
   Note that the results may differ from the ones in
   the VCS log since empty change sets (such as merges) are
   ignored in the mining." 
  [["number-of-commits" (count (entities/all-revisions ds))]
   ["number-of-entities" (count (entities/all ds))]
   ["number-of-entities-changed" (ds/-nrows ds)]
   ["number-of-authors" (count (authors/all ds))]])

(defn overview
  [ds & _]
  (ds/-dataset [:statistic :value]
               (calculate-summary ds)))