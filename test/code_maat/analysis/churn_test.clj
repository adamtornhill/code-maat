;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.churn-test
  (:require [code-maat.analysis.churn :as churn]
            [code-maat.analysis.test-data :as test-data]
            [incanter.core :as incanter])
  (:use clojure.test))

(def ^:const options {})

(def ^:const incomplete
   (incanter/to-dataset
    [{:entity "A" :rev 1 :author "at" :date "2013-11-10"}
     {:entity "B" :rev 2 :author "ta" :date "2013-11-11"}]))

(def ^:const simple
   (incanter/to-dataset
    [{:entity "A" :rev 1 :author "at" :date "2013-11-10" :loc-added "10" :loc-deleted "1"}
     {:entity "B" :rev 2 :author "ta" :date "2013-11-11" :loc-added "20" :loc-deleted "2"}
     {:entity "B" :rev 3 :author "at" :date "2013-11-11" :loc-added "2" :loc-deleted "0"}]))

(def ^:const with-binary
   (incanter/to-dataset
    [{:entity "binary" :rev 1 :author "at" :date "2013-11-10" :loc-added "-" :loc-deleted "-"}]))

(deftest throws-error-on-missing-modification-info
  "Some VCS (e.g. hg) don't provide the necessary metrics.
   In case a churn analysis is requested on such incomplete
   data we want to detect it early."
  (is (thrown? IllegalArgumentException
               (churn/absolutes-trend incomplete options))))

(deftest calculates-absolute-churn-by-date
  (is (= (churn/absolutes-trend simple options)
         (incanter/to-dataset
          [{:date "2013-11-10" :added 10 :deleted 1}
           {:date "2013-11-11" :added 22 :deleted 2}]))))

(deftest binaries-are-counted-as-zero-churn
  "There are simply no statistics from the VCS for these."
  (is  (= (churn/absolutes-trend with-binary options)
          (incanter/to-dataset
           [{:date "2013-11-10" :added 0 :deleted 0}]))))
