;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.effort-test
  (:require [code-maat.analysis.effort :as effort]
            [code-maat.dataset.dataset :as ds])
  (:use clojure.test))

(def ^:const options {})

(ds/def-ds single-effort
  [{:entity "A" :rev 1 :author "at" :date "2013-11-10"}
   {:entity "B" :rev 2 :author "at" :date "2013-11-11"}
   {:entity "B" :rev 3 :author "at" :date "2013-11-15"}])

(ds/def-ds multi-effort
  [{:entity "A" :rev 1 :author "at" :date "2013-11-10"}
   {:entity "A" :rev 2 :author "xy" :date "2013-11-11"}
   {:entity "A" :rev 3 :author "zt" :date "2013-11-15"}])

(deftest calculates-effort-for-single-author
  (is (= (effort/as-revisions-per-author single-effort options)
         (ds/-dataset [:entity :author :author-revs :total-revs]
                      [["A" "at" 1 1]
                       ["B" "at" 2 2]]))))

(deftest calculates-effort-for-multiple-authors
  (is (= (effort/as-revisions-per-author multi-effort options)
         (ds/-dataset [:entity :author :author-revs :total-revs]
                      [["A" "at" 1 3]
                       ["A" "xy" 1 3]
                       ["A" "zt" 1 3]]))))

(deftest calculates-entity-fragmentation-for-single-author
  "The fractal value is a measurement of how
   distributed the effort on a specific entity is."
  (is (= (effort/as-entity-fragmentation single-effort options)
         (ds/-dataset [:entity :fractal-value :total-revs]
                      [["B" 0.00 2]
                       ["A" 0.00 1]]))))

(deftest calculates-entity-fragmentation-for-multiple-authors
  (is (= (effort/as-entity-fragmentation multi-effort options)
         (ds/-dataset [:entity :fractal-value :total-revs]
                      [["A" 0.67 3]]))))

(ds/def-ds shared-effort
  [{:entity "A" :rev 1 :author "zt" :date "2013-11-10"}
   {:entity "A" :rev 2 :author "at" :date "2013-11-11"}
   {:entity "A" :rev 3 :author "at" :date "2013-11-15"}
   {:entity "B" :rev 4 :author "xx" :date "2013-11-15"}
   {:entity "C" :rev 5 :author "x1" :date "2013-11-16"}
   {:entity "C" :rev 6 :author "x2" :date "2013-11-16"}])

(deftest identifies-main-developer-by-revisions
  (is (= (effort/as-main-developer-by-revisions shared-effort options)
         (ds/-dataset [:entity :main-dev :added :total-added :ownership]
                      [["A" "at" 2  3 0.67]
                       ["B" "xx" 1  1 1.0]
                       ["C" "x1" 1  2 0.5]]))))
