;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.test-data
  (:require [incanter.core :as incanter]))

(def ^:const vcs [{:author "apt" :entity "A" :rev 1 :message "Some change"}
                  {:author "apt" :entity "B" :rev 1 :message "Another change"}
                  {:author "apt" :entity "A" :rev 2 :message "Second change"}
                  {:author "jt"  :entity "A" :rev 3 :message "Third change"}])
(def ^:const vcsd (incanter/to-dataset vcs))

;;; Defines a dataset with a single entry to test one border case.
(def ^:const single-vcs [{:author "apt" :entity "A" :rev 1}])
(def ^:const single-vcsd (incanter/to-dataset single-vcs))

(def ^:const empty-vcsd (incanter/to-dataset []))

(defn content-of [ds]
  (:rows (incanter/sel
          ds
          :rows :all)))

(def ^:const options-with-low-thresholds
  "Typically given as cmd line args"
  {:min-revs 1
   :min-shared-revs 1
   :min-coupling 50
   :max-coupling 100
   :max-changeset-size 10})
