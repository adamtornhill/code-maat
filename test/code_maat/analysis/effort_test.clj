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
