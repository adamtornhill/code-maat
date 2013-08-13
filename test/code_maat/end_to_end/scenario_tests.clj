(ns code-maat.end-to-end.scenario-tests
  (:require [code-maat.app.app :as app])
  (:use clojure.test))

;;; Test: write a scenario for each supported:
;;;  - vcs
;;;  - text-based output
;;; Ensure that all combinations are run!
;;; Make the test data-driven!

(def ^:const svn-log-file "./test/code_maat/end_to_end/simple.xml")

(def ^:const svn-csv-options
  {:module "svn"
   :output "csv"
   :rows 1}) ; TODO - make sure it works with a larger number too!!

;; TODO: test when we have real csv!
(deftest generates-csv-summary-from-svn-log-file
  (app/run svn-log-file svn-csv-options))
