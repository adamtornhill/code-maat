;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.logical-coupling
  (:require [code-maat.analysis.coupling-algos :as c]
            [code-maat.dataset.dataset :as ds]
            [code-maat.analysis.math :as m]
            [clojure.math.numeric-tower :as math]
            [incanter.core :as incanter]))

;;; This module calculates the logical coupling of all modules.
;;;
;;; Logical coupling refers to modules that tend to change together.
;;; It's information that's recorded in our version-control systems (VCS).
;;;
;;; Input: all analysis expect an Incanter dataset with (at least) the following columns:
;;; :entity :rev
;;;
;;; Oputput: the analysis returns an Incanter dataset with the following columns:
;;; :entity :coupled :degree :average-revs

(defn- as-logical-coupling-measure
  "This is where the result is assembled.
   We already have all the data. Now we just pass through the
   coupled modules, with their co-change frequencies, and
   transform it to a degree of coupling.
   The coupling formula is simple: the number of shared
   revisions divided by the average number of revisions for
   the two coupled modules."
  [ds options within-threshold-fn?]
  (let [co-changing (c/co-changing-by-revision ds options)
        module-revs (c/module-by-revs co-changing)
        coupling (c/coupling-frequencies co-changing)]
    (for [[[first-entity second-entity] shared-revs] coupling
          :let [first-entity-revisions (module-revs first-entity)
                second-entity-revisions (module-revs second-entity)
                average-revs (m/average first-entity-revisions
                                        second-entity-revisions)
                coupling (m/as-percentage (/ shared-revs average-revs))]
          :when (within-threshold-fn? average-revs shared-revs coupling)]
      {:entity first-entity
       :coupled second-entity
       :degree (int coupling)
       :average-revs  (math/ceil average-revs)
       ; verbose options:
       :first-entity-revisions first-entity-revisions
       :second-entity-revisions second-entity-revisions
       :shared-revisions shared-revs})))

(defn- results-depending-on
  [{:keys [verbose-results] :as _options}
   results]
  (let [coupling-results [:entity :coupled :degree :average-revs]
        verbose-details  [:first-entity-revisions :second-entity-revisions :shared-revisions]]
    (if verbose-results
      (ds/-dataset (into coupling-results verbose-details) results)
      (ds/-dataset coupling-results results))))

(defn by-degree
  "Calculates the degree of logical coupling. Returns a seq
   sorted in descending order (default) or an optional, custom sorting criterion.
   The calulcation is  based on the given coupling statistics.
   The coupling is calculated as a percentage value based on
   the number of shared commits between coupled entities divided
   by the average number of total commits for the coupled entities."
  ([ds options]
   (by-degree ds options :desc))
  ([ds options order-fn]
   (->>
    (partial c/within-threshold? options)
    (as-logical-coupling-measure ds options)
    (results-depending-on options)
    (incanter/$order [:degree :average-revs] order-fn))))
