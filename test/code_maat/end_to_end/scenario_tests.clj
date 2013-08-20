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

(def ^:const empty-log-file "./test/code_maat/end_to_end/empty.xml")

(def ^:const statsvn-log-file "./test/code_maat/end_to_end/statsvn.log")

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

(defn- run-with-str-output [log-file options]
  (with-out-str
    (app/run log-file options)))

;;; Could really benefit from being data-driven!
(deftest generates-csv-summary-from-svn-log-file
  (is (= (run-with-str-output svn-log-file (svn-csv-options 10 "authors"))
         "entity,n-authors,n-revs\n/Infrastrucure/Network/Connection.cs ,2,2\n/Presentation/Status/ClientPresenter.cs ,1,1\n"))
  (is (= (run-with-str-output svn-log-file (svn-csv-options 10 "revisions"))
         "entity,n-revs\n/Infrastrucure/Network/Connection.cs ,2\n/Presentation/Status/ClientPresenter.cs ,1\n"))
  (is (= (run-with-str-output svn-log-file (svn-csv-options 10 "coupling"))
         "entity,coupled,degree,average-revs\n/Presentation/Status/ClientPresenter.cs ,/Infrastrucure/Network/Connection.cs ,200/3,3/2\n/Infrastrucure/Network/Connection.cs ,/Presentation/Status/ClientPresenter.cs ,200/3,3/2\n")))

(deftest parses-live-data
  (testing "StatSvn: this file has a different format (no kind-attribute on the paths)"
    (is (= (run-with-str-output statsvn-log-file (svn-csv-options 2 "authors"))
           "entity,n-authors,n-revs\n/trunk/statsvn/site/changes.xml,2,4\n/trunk/statsvn/project.properties,1,4\n"))))

(deftest ignores-entities-before-the-start-date
  (is (= (run-with-str-output svn-log-file
           (with-date-limit
             (clj-time/date-time 2013 03 01) ; after the last entry
             (svn-csv-options 10 "authors")))
         "entity,n-authors,n-revs\n")))

(deftest reports-invalid-arguments
  (testing "Non-existent input file"
    (is (thrown? IllegalArgumentException (app/run "I/do/not/exist")))))

(deftest boundary-cases
  (testing "Empty input gives empty analysis results"
    (is (= (run-with-str-output empty-log-file {:analysis "authors"})
           "entity,n-authors,n-revs\n"))
     (is (= (run-with-str-output empty-log-file {:analysis "revisions"})
            "entity,n-revs\n"))
      (is (= (run-with-str-output empty-log-file {:analysis "coupling"})
           "col-0\n")))) ; not perfect, but perhaps good enough for now...
    
