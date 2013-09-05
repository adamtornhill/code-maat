;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.end-to-end.scenario-tests
  (:require [code-maat.app.app :as app]
            [code-maat.analysis.test-data :as test-data]
            [code-maat.test.data-driven :as dd])
  (:use clojure.test))

;;; This module contains end-to-end tests running the whole app
;;; from fron-end to back-end.
;;; Most test cases are data-driven; I parameterize them with
;;; the options (i.e. specify a VCS) and a log file for that
;;; particular option. This strategy is based on the content
;;; of the specified log files below. The log files contain
;;; the same information, just on different VCS formats.

(def ^:const svn-log-file "./test/code_maat/end_to_end/simple.xml")
(def ^:const git-log-file "./test/code_maat/end_to_end/simple_git.txt")
(def ^:const hg-log-file "./test/code_maat/end_to_end/simple_hg.txt")

(def ^:const empty-log-file "./test/code_maat/end_to_end/empty.xml")
(def ^:const empty-git-file "./test/code_maat/end_to_end/empty.git")
(def ^:const empty-hg-file "./test/code_maat/end_to_end/empty.hg")

(defn- svn-csv-options
  [analysis]
  (merge
   test-data/options-with-low-thresholds
   {:version-control "svn"
    :analysis analysis}))

(defn- git-options
  [analysis]
   (merge
   test-data/options-with-low-thresholds
   {:version-control "git"
    :analysis analysis}))

(defn- hg-options
  [analysis]
   (merge
   test-data/options-with-low-thresholds
   {:version-control "hg"
    :analysis analysis}))

(defn- run-with-str-output [log-file options]
  (with-out-str
    (app/run log-file options)))

(defmacro def-data-driven-with-vcs-test
  "Encapsulates the common pattern of iterating over a data driven
   test providing a vector of [file options] for each item.
   The body will execute with the symbols log-file and options bound to
   the different options in the test-data."
  [name test-data & body]
  `(dd/def-dd-test ~name
     [~'ddval# ~test-data]
     (let [[~'log-file ~'options] ~'ddval#]
       ~@body)))

(def-data-driven-with-vcs-test analysis-of-authors
  [[svn-log-file (svn-csv-options "authors")]
   [git-log-file (git-options "authors")]
   [hg-log-file (hg-options "authors")]]
  (is (= (run-with-str-output log-file options)
         "entity,n-authors,n-revs\n/Infrastrucure/Network/Connection.cs,2,2\n/Presentation/Status/ClientPresenter.cs,1,1\n")))

(def-data-driven-with-vcs-test analysis-of-revisions
  [[svn-log-file (svn-csv-options "revisions")]
   [git-log-file (git-options "revisions")]
   [hg-log-file (hg-options "revisions")]]
  (is (= (run-with-str-output log-file options)
         "entity,n-revs\n/Infrastrucure/Network/Connection.cs,2\n/Presentation/Status/ClientPresenter.cs,1\n")))

(def-data-driven-with-vcs-test analysis-of-coupling
  [[svn-log-file (svn-csv-options "coupling")]
   [git-log-file (git-options "coupling")]
   [hg-log-file (hg-options "coupling")]]
  (is (= (run-with-str-output log-file options)
         "entity,coupled,degree,average-revs\n/Presentation/Status/ClientPresenter.cs,/Infrastrucure/Network/Connection.cs,66,2\n/Infrastrucure/Network/Connection.cs,/Presentation/Status/ClientPresenter.cs,66,2\n")))

(deftest reports-invalid-arguments
  (testing "Non-existent input file"
    (is (thrown? IllegalArgumentException (app/run "I/do/not/exist"))))
  (testing "Missing mandatory options (normally added by the front)"
    (is (thrown? IllegalArgumentException (app/run svn-log-file {:analysis "coupling"})))))

(def-data-driven-with-vcs-test analysis-of-authors-with-empty-log
  [[empty-log-file (svn-csv-options "authors")]
   [empty-git-file (git-options "authors")]
   [empty-hg-file (hg-options "authors")]]
  (is (= (run-with-str-output log-file options)
         "entity,n-authors,n-revs\n")))

(def-data-driven-with-vcs-test analysis-of-coupling-with-empty-log
  [[empty-log-file (svn-csv-options "coupling")]
   [empty-git-file (git-options "coupling")]
   [empty-hg-file (hg-options "coupling")]]
  (is (= (run-with-str-output log-file options)
         "entity,coupled,degree,average-revs\n")))

(def-data-driven-with-vcs-test analysis-of-revisions-with-empty-log
  [[empty-log-file (svn-csv-options "revisions")]
   [empty-git-file (git-options "revisions")]
   [empty-hg-file (hg-options "revisions")]]
  (is (= (run-with-str-output log-file options)
         "entity,n-revs\n")))
    
