;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.churn-test
  (:require [code-maat.analysis.churn :as churn]
            [code-maat.dataset.dataset :as ds]
            [incanter.core :as incanter])
  (:use clojure.test))

(def ^:const options {})

(ds/def-ds incomplete
  [{:entity "A" :rev 1 :author "at" :date "2013-11-10"}
   {:entity "B" :rev 2 :author "ta" :date "2013-11-11"}])

(ds/def-ds single-author
  [{:entity "Same" :rev 1 :author "single" :date "2013-11-10" :loc-added "10" :loc-deleted "1"}
   {:entity "Same" :rev 2 :author "single" :date "2013-11-11" :loc-added "20" :loc-deleted "2"}
   {:entity "Same" :rev 3 :author "single" :date "2013-11-11" :loc-added "2" :loc-deleted "0"}])

;; Of course, an author doesn't have to add lines. In that case we
;; need to  take care since we get a total of zero => special case.
(ds/def-ds only-removed-lines
  [{:entity "Same" :rev 1 :author "single" :date "2013-11-10" :loc-added "0" :loc-deleted "1"}])

(ds/def-ds only-added-lines
  [{:entity "Same" :rev 1 :author "single" :date "2013-11-10" :loc-added "1" :loc-deleted "0"}])

(ds/def-ds simple
  [{:entity "B" :rev 2 :author "ta" :date "2013-11-11" :loc-added "20" :loc-deleted "2"}
   {:entity "A" :rev 1 :author "at" :date "2013-11-10" :loc-added "10" :loc-deleted "1"}
   {:entity "B" :rev 3 :author "at" :date "2013-11-11" :loc-added "2" :loc-deleted "0"}])

(ds/def-ds same-author
  [{:entity "A" :rev 1 :author "at" :date "2013-11-10" :loc-added "10" :loc-deleted "1"}
   {:entity "A" :rev 2 :author "at" :date "2013-11-11" :loc-added "2" :loc-deleted "5"}
   {:entity "A" :rev 3 :author "xy" :date "2013-11-11" :loc-added "7" :loc-deleted "1"}
   {:entity "A" :rev 4 :author "xy" :date "2013-11-11" :loc-added "8" :loc-deleted "2"}])

(ds/def-ds with-binary
  [{:entity "binary" :rev 1 :author "at" :date "2013-11-10" :loc-added "-" :loc-deleted "-"}])

(defn- as-ds
  "Returns a dataset with a fixed column order."
  [v]
  (ds/-dataset [:date :added :deleted] v))

(deftest throws-error-on-missing-modification-info
  "Some VCS (e.g. hg) don't provide the necessary metrics.
   In case a churn analysis is requested on such incomplete
   data we want to detect it early."
  (is (thrown? IllegalArgumentException
               (churn/absolutes-trend incomplete options))))

(deftest calculates-absolute-churn-by-date
  (is (= (churn/absolutes-trend simple options)
         (as-ds
          [{:date "2013-11-10" :added 10 :deleted 1}
           {:date "2013-11-11" :added 22 :deleted 2}]))))

(deftest binaries-are-counted-as-zero-churn
  "There are simply no statistics from the VCS for these."
  (is  (= (churn/absolutes-trend with-binary options)
          (as-ds [{:date "2013-11-10" :added 0 :deleted 0}]))))

(deftest calculates-churn-by-author
  "Get an overview of individual contributions."
  (is (= (churn/by-author simple options)
         (ds/-dataset [:author :added :deleted]
                      [{:author "at" :added 12 :deleted 1}
                       {:author "ta" :added 20 :deleted 2}]))))

(deftest calculates-churn-by-entity
  "Identify entities with the highest churn rate."
  (is (= (churn/by-entity simple options)
         (ds/-dataset [:entity :added :deleted]
                      [{:entity "B" :added 22 :deleted 2}
                       {:entity "A" :added 10 :deleted 1}]))))

(deftest calculates-author-ownership-from-churn
  (is (= (churn/as-ownership simple options)
          (ds/-dataset [:entity :author :added :deleted]
           [["A" "at" 10 1]
            ["B" "ta" 20 2]
            ["B" "at" 2 0]]))))

(deftest sums-ownership-churn-for-same-author
  "We want an aggregated number when the same author makes multiple mods."
  (is (= (churn/as-ownership same-author options)
         (ds/-dataset [:entity :author :added :deleted]
          [["A" "at" 12 6]
           ["A" "xy" 15 3]]))))

;;; Tests of the main developer algorithm

(defn- as-main-dev-ds
  [v]
  (ds/-dataset [:entity :main-dev :added :total-added :ownership] v))

(deftest identifies-single-main-developer
  "A main developer is the one who conributed most code.
   If there's only one, single developer it's the obvious owner."
  (is (= (churn/by-main-developer single-author options)
         (as-main-dev-ds [["Same" "single" 32 32 1.0]]))))

(deftest identifies-main-developer-on-shared-entities
  (is (= (churn/by-main-developer same-author options)
         (as-main-dev-ds [["A" "xy" 15 27 0.56]]))))

(deftest ownership-is-none-without-added-lines
  (is (= (churn/by-main-developer only-removed-lines options)
         (as-main-dev-ds [["Same" "single" 0 0 0.00]]))))

;;; Tests of main developer algorithms for a refactoring developer

(defn- as-refactor-ds
  [v]
  (ds/-dataset [:entity :main-dev :removed :total-removed :ownership] v))

(deftest identifies-refactoring-developer-on-shared-entities
  (is (= (churn/by-refactoring-main-developer same-author options)
         (as-refactor-ds [["A" "at" 6 9 0.67]]))))

(deftest ownership-is-none-without-removed-lines
  (is (= (churn/by-refactoring-main-developer only-added-lines options)
         (as-refactor-ds [["Same" "single" 0 0 0.00]]))))
