(ns code-maat.end-to-end.scenario-tests
  (:require [code-maat.parsers.svn :as svn]
            [code-maat.parsers.xml :as xml-parser]
            [code-maat.app.app :as app])
  (:use clojure.test))

;;; Test: write a scenario for each supported:
;;;  - vcs
;;;  - text-based output
;;; Ensure that all combinations are run!

(defn- replace-with-a-stream! [ds]
  ds)

;; TODO: test when we have real csv!
(deftest generates-csv-summary-from-svn-log-file
  (app/run "./test/code_maat/end_to_end/simple.xml" replace-with-a-stream!))
