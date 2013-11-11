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

(deftest calculates-churn-by-author
  "The total churn of each individual contributor"
  (is (= (run-with-str-output git-log-file 
                              {:version-control "git"
                               :analysis "author-churn"})
         "author,added,deleted\nAPT,4,6\nXYZ,18,2\n")))

(deftest calculates-churn-by-entity
  "Identify entities with the highest churn rate."
  (is (= (run-with-str-output git-log-file 
                              {:version-control "git"
                               :analysis "entity-churn"})
         "entity,added,deleted\n/Infrastrucure/Network/Connection.cs,19,4\n/Presentation/Status/ClientPresenter.cs,3,4\n")))
