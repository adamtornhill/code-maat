;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.authors-test
  (:require [code-maat.analysis.authors :as authors]
            [code-maat.analysis.test-data :as test-data]
            [incanter.core :as incanter])
  (:use clojure.test))

(deftest deduces-all-authors
  (is (= (into #{} (authors/all test-data/vcsd))
         #{"apt" "jt"})))

(deftest gives-all-authors-of-specified-module
  (is (= (incanter/to-list (authors/of-module "A" test-data/vcsd))
         [["apt"] ["jt"]]))
  (is (= (incanter/to-list (authors/of-module "B" test-data/vcsd))
         [["apt"]])))

(deftest sorts-entities-on-max-number-of-authors
  (is (= (test-data/content-of (authors/by-count
                                test-data/vcsd
                                test-data/options-with-low-thresholds))
         [{:n-authors 2 :entity "A" :n-revs 3}
          {:n-authors 1 :entity "B" :n-revs 1}])))

(deftest sorts-order-is-optional
  (is (= (test-data/content-of (authors/by-count
                                test-data/vcsd
                                test-data/options-with-low-thresholds
                                :asc))
         [{:n-authors 1 :entity "B" :n-revs 1}
          {:n-authors 2 :entity "A" :n-revs 3}])))
         
         
