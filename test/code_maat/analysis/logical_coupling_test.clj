;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.logical-coupling-test
  (:require [code-maat.analysis.logical-coupling :as coupling]
            [code-maat.analysis.test-data :as test-data]
            [incanter.core :as incanter])
  (:use clojure.test))

(def ^:const single-entity-commit
  [{:entity "This/is/a/single/entity" :rev 1}])

(def ^:const one-revision
  [{:entity "A" :rev 1}
   {:entity "B" :rev 1}
   {:entity "C" :rev 1}])

(def ^:const revd (incanter/to-dataset one-revision))

(def ^:const coupled
  [{:entity "A" :rev 1}
   {:entity "B" :rev 1}
   {:entity "C" :rev 1}
   {:entity "A" :rev 2}
   {:entity "B" :rev 2}])

(def ^:const coupledd (incanter/to-dataset coupled))

(def ^:const revd (incanter/to-dataset one-revision))

(deftest calculates-coupling-by-degree
  (is (= (incanter/to-list (coupling/by-degree
                            coupledd
                            test-data/options-with-low-thresholds))
         ;; :entity :coupled :degree :average-revs
         [["A"      "B"       100   2]
          ["A"      "C"       66    2]
          ["B"      "C"       66    2]])))

(deftest gives-empty-result-for-single-change-set-with-single-entity
  "A single change set with a single entity (boundary case)"
  (is (= (incanter/to-list (coupling/by-degree
                            (incanter/to-dataset single-entity-commit)
                            test-data/options-with-low-thresholds))
         [])))
