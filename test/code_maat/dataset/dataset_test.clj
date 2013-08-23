;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.dataset.dataset-test
  (:require [code-maat.dataset.dataset :as ds]
            [incanter.core :as incanter]
            [code-maat.analysis.test-data :as test-data])
  (:use clojure.test))

(deftest recognizes-empty-dataset
  (is (ds/-empty? test-data/empty-vcsd))
  (is (not (ds/-empty? test-data/vcsd))))

(deftest groups-by-given-column
  (let [group (ds/-group-by :entity test-data/vcsd)]
    (is (= (keys group)
           [{:entity "A"} {:entity "B"}]))))

(deftest selects-by-given-column
  (testing "Multiple rows in dataset"
    (is (= (ds/-select-by :entity test-data/vcsd)
           ["A" "B" "A" "A"])))
  (testing "Single row dataset"
    (is (= (ds/-select-by :entity test-data/single-vcsd)
           ["A"])))
  (testing "No rows"
    (is (= (ds/-select-by :entity test-data/empty-vcsd)
           []))))

(deftest counts-rows
  (is (= (ds/-nrows test-data/vcsd)
         4)))