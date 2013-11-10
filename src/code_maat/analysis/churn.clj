;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.churn
  (:require [code-maat.dataset.dataset :as ds]
            [code-maat.analysis.entities :as entities]))

;;; This module contains functions for calculating churn metrics.
;;; Code churn is related to the quality of modules; the higher
;;; the churn, the more post-release defects.
;;; Further, inspecting the churn trend lets us spot certain
;;; organization-oriented patterns. For example, we may spot
;;; integration bottlenecks as spikes just before the end of
;;; one iteration.

(defn absolutes-trend
  [ds options]
  (throw
   (IllegalArgumentException.
    (str "churn analysis: the given VCS data doesn't contain modification metrics."
         "Check the code-maat docs for supported VCS and correct log format."))))
