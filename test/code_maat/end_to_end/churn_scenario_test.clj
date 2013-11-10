(ns code-maat.end-to-end.churn-scenario-test
  (:require [code-maat.app.app :as app]
            [code-maat.analysis.test-data :as test-data]
            [code-maat.test.data-driven :as dd])
  (:use clojure.test))

;;; This module contains end-to-end tests running the whole app
;;; from front-end to back-end with respect to code churn.

(def ^:const git-log-file "./test/code_maat/end_to_end/simple_git.txt")

(defn- run-with-str-output [log-file options]
  (with-out-str
    (app/run log-file options)))

(deftest calculates-absolute-churn
  (is (= (run-with-str-output git-log-file 
                              {:version-control "git"
                               :analysis "abs-churn"})
         "date,added,deleted\n2013-02-08,4,6\n2013-02-07,18,2\n")))
