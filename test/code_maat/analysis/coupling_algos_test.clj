;;; Copyright (C) 2014 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.coupling-algos-test
  (:require [code-maat.analysis.coupling-algos :as coupling]
            [code-maat.analysis.test-data :as test-data]
            [incanter.core :as incanter])
  (:use clojure.test))

(def ^:const single-entity-commit
  [{:entity "This/is/a/single/entity" :rev 1}])

(def ^:const one-revision
  [{:entity "A" :rev 1}
   {:entity "B" :rev 1}
   {:entity "C" :rev 1}])

(def ^:const single (incanter/to-dataset one-revision))

(def ^:const coupled
  [{:entity "A" :rev 1}
   {:entity "B" :rev 1}
   {:entity "C" :rev 1}
   {:entity "A" :rev 2}
   {:entity "B" :rev 2}])

(def ^:const multiple (incanter/to-dataset coupled))

(def ^:const expected-multiple-co-changes
  [[["A" "A"] ["A" "B"] ["A" "C"] ["B" "B"] ["B" "C"] ["C" "C"]]
   [["A" "A"] ["A" "B"] ["B" "B"]]])

(deftest identifies-couples-in-a-revision
  "Note that this step in the algorithm maintains identity couples.
   We use them later to sum-up the the total number of revisions of
   a module."
  (is (= (coupling/co-changing-by-revision single test-data/options-with-low-thresholds)
         [[["A" "A"] ["A" "B"] ["A" "C"] ["B" "B"] ["B" "C"] ["C" "C"]]])))

(deftest identifies-couples-in-multiple-revisions
  (is (= (coupling/co-changing-by-revision multiple test-data/options-with-low-thresholds)
         expected-multiple-co-changes)))

(deftest calculates-coupling-frequencies
  (is (= (coupling/coupling-frequencies expected-multiple-co-changes)
         [[["A" "B"] 2] [["A" "C"] 1] [["B" "C"] 1]])))

(deftest calculates-module-change-freqs
  (is (= (coupling/module-by-revs expected-multiple-co-changes)
         {"A" 2, "B" 2, "C" 1})))
