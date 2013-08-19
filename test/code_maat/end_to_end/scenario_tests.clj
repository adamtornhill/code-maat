(ns code-maat.end-to-end.scenario-tests
  (:require [code-maat.app.app :as app]
            [clj-time.core :as clj-time]
            [code-maat.analysis.test-data :as test-data])
  (:use clojure.test))

;;; Test: write a scenario for each supported:
;;;  - vcs
;;;  - text-based output
;;; Ensure that all combinations are run!
;;; TODO; Make the test data-driven!

(def ^:const svn-log-file "./test/code_maat/end_to_end/simple.xml")

(defn- svn-csv-options
  ([rows analysis]
     (merge
      test-data/options-with-low-thresholds
      {:module "svn"
       :output "csv"
       :rows rows
       :analysis analysis
       :max-entries 10})))

(defn- with-date-limit [date options]
  (merge {:date date} options))

(defn- run-with-str-output [options]
  (with-out-str
    (app/run svn-log-file options)))

;;; Could really benefit from being data-driven!
(deftest generates-csv-summary-from-svn-log-file
  (is (= (run-with-str-output (svn-csv-options 10 "authors"))
         "entity,n-authors,n-revs\n/Infrastrucure/Network/Connection.cs ,2,2\n/Presentation/Status/ClientPresenter.cs ,1,1\n"))
  (is (= (run-with-str-output (svn-csv-options 10 "revisions"))
         "entity,n-revs\n/Infrastrucure/Network/Connection.cs ,2\n/Presentation/Status/ClientPresenter.cs ,1\n"))
  (is (= (run-with-str-output (svn-csv-options 10 "coupling"))
         "entity,coupled,degree,average-revs\n/Presentation/Status/ClientPresenter.cs ,/Infrastrucure/Network/Connection.cs ,200/3,3/2\n/Infrastrucure/Network/Connection.cs ,/Presentation/Status/ClientPresenter.cs ,200/3,3/2\n")))

(deftest ignores-entities-before-the-start-date
  (is (= (run-with-str-output
           (with-date-limit
             (clj-time/date-time 2013 03 01) ; after the last entry
             (svn-csv-options 10 "authors")))
         "entity,n-authors,n-revs\n")))
    
