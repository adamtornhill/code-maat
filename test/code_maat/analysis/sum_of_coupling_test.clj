;;; Copyright (C) 2014 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.sum-of-coupling-test
  (:require [code-maat.analysis.sum-of-coupling :as coupling]
            [code-maat.analysis.test-data :as test-data]
            [incanter.core :as incanter])
  (:use clojure.test))

(def ^:const coupled
  [{:entity "A" :rev 1}
   {:entity "B" :rev 1}
   {:entity "C" :rev 1}
   {:entity "A" :rev 2}
   {:entity "B" :rev 2}])

(def ^:const multiple (incanter/to-dataset coupled))

(deftest measures-coupling-by-entity
  (is (= (coupling/as-soc
          multiple
          test-data/options-with-low-thresholds)
         [["A" 3] ["B" 3] ["C" 2]])))
