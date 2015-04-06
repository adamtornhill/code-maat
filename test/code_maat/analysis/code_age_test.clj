;;; Copyright (C) 2015 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.code-age-test
  (:require [code-maat.analysis.code-age :as analysis]
            [code-maat.analysis.test-data :as td]
            [incanter.core :as incanter]
            [code-maat.dataset.dataset :as ds])
  (:use clojure.test))

(def ^:const vcs [{:entity "A" :rev 1 :date "2013-12-25"}
                  {:entity "B" :rev 1 :date "2013-12-31"}
                  {:entity "A" :rev 2 :date "2014-02-28"}
                  {:entity "A" :rev 3 :date "2014-04-05"}])
(def ^:const vcsd (incanter/to-dataset vcs))

(defn- as-now
  "To make the tests deterministic we need to specify what
   _now_ really means. This is done by a comand line argument."
  [fixed-date]
  {:age-time-now fixed-date})

(defn- as-age-ds
  [result]
  (ds/-dataset [:entity :age-months] result))

(deftest calculates-age-by-last-modification-date
  (is (= (analysis/by-age vcsd (as-now "2014-04-06"))
         (as-age-ds [["A" 0] ["B" 3]]))))

(deftest code-gets-older-as-time-passes-by
  (testing "One month into the future"
    (is (= (analysis/by-age vcsd (as-now "2014-05-06"))
           (as-age-ds [["A" 1] ["B" 4]]))))
  (testing "A year into the future"
    (is (= (analysis/by-age vcsd (as-now "2015-04-06"))
           (as-age-ds [["A" 12] ["B" 15]])))))

;;; This is tricky - if we move back in time we need to
;;; ignore all commits that happened after the given time.
(deftest code-was-younger-in-the-past
  (testing "One month in the past"
    (is (= (analysis/by-age vcsd (as-now "2014-03-06"))
           (as-age-ds [["A" 0] ["B" 2]]))))
  (testing "Before the B module was introduced (should be ignored)"
     (is (= (analysis/by-age vcsd (as-now "2013-12-26"))
           (as-age-ds [["A" 0]])))))
         
