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

(def ^:const vcs [{:entity "A" :rev 1 :date "2014-12-25"}
                  {:entity "B" :rev 1 :date "2014-12-31"}
                  {:entity "A" :rev 2 :date "2015-02-28"}
                  {:entity "A" :rev 3 :date "2015-04-05"}])
(def ^:const vcsd (incanter/to-dataset vcs))

(def ^:const no-options {})

(defn- as-age-ds
  [result]
  (ds/-dataset [:entity :age-months] result))

(deftest calculates-age-by-last-modification-date
  (is (= (analysis/by-age vcsd no-options)
         (as-age-ds [["A" 0] ["B" 3]]))))
         
