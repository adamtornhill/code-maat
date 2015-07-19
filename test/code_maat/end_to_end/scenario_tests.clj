;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.end-to-end.scenario-tests
  (:require [code-maat.app.app :as app]
            [code-maat.analysis.test-data :as test-data]
            [code-maat.test.data-driven :as dd])
  (:use [clojure.test]
        [code-maat.tools.test-tools]))

;;; This module contains end-to-end tests running the whole app
;;; from fron-end to back-end.
;;; Most test cases are data-driven; I parameterize them with
;;; the options (i.e. specify a VCS) and a log file for that
;;; particular option. This strategy is based on the content
;;; of the specified log files below. The log files contain
;;; the same information, just on different VCS formats.

(def ^:const svn-log-file "./test/code_maat/end_to_end/simple.xml")
(def ^:const git-log-file "./test/code_maat/end_to_end/simple_git.txt")
(def ^:const git2-log-file "./test/code_maat/end_to_end/simple_git2.txt")
(def ^:const hg-log-file "./test/code_maat/end_to_end/simple_hg.txt")
(def ^:const p4-log-file "./test/code_maat/end_to_end/simple_p4.txt")

(def ^:const empty-log-file "./test/code_maat/end_to_end/empty.xml")
(def ^:const empty-git-file "./test/code_maat/end_to_end/empty.git")
(def ^:const empty-hg-file "./test/code_maat/end_to_end/empty.hg")
(def ^:const empty-p4-file "./test/code_maat/end_to_end/empty.p4")

(def shared-options
  (merge
   test-data/options-with-low-thresholds
   {:age-time-now "2015-03-01"}))

(defn- make-options-for
  [vcs analysis]
  (merge
   shared-options
   {:version-control vcs
    :analysis analysis}))

(defn- svn-csv-options
  [analysis]
  (make-options-for "svn" analysis))

(defn- git-options
  [analysis]
  (make-options-for "git" analysis))

(defn- git2-options
  [analysis]
  (make-options-for "git2" analysis))

(defn- hg-options
  [analysis]
  (make-options-for "hg" analysis))

(defn- p4-options
  [analysis]
  (make-options-for "p4" analysis))

(def-data-driven-with-vcs-test analysis-of-authors
  [[svn-log-file (svn-csv-options "authors")]
   [git-log-file (git-options "authors")]
   [git2-log-file (git2-options "authors")]
   [p4-log-file (p4-options "authors")]
   [hg-log-file (hg-options "authors")]]
  (is (= (run-with-str-output log-file options)
         "entity,n-authors,n-revs\n/Infrastrucure/Network/Connection.cs,2,2\n/Presentation/Status/ClientPresenter.cs,1,1\n")))

(def-data-driven-with-vcs-test analysis-of-revisions
  [[svn-log-file (svn-csv-options "revisions")]
   [git-log-file (git-options "revisions")]
   [git2-log-file (git2-options "revisions")]
   [p4-log-file (p4-options "revisions")]
   [hg-log-file (hg-options "revisions")]]
  (is (= (run-with-str-output log-file options)
         "entity,n-revs\n/Infrastrucure/Network/Connection.cs,2\n/Presentation/Status/ClientPresenter.cs,1\n")))

(def-data-driven-with-vcs-test analysis-of-coupling
  [[svn-log-file (svn-csv-options "coupling")]
   [git-log-file (git-options "coupling")]
   [git2-log-file (git2-options "coupling")]
   [p4-log-file (p4-options "coupling")]
   [hg-log-file (hg-options "coupling")]]
  (is (= (run-with-str-output log-file options)
         "entity,coupled,degree,average-revs\n/Infrastrucure/Network/Connection.cs,/Presentation/Status/ClientPresenter.cs,66,2\n")))

(def-data-driven-with-vcs-test analysis-of-effort
  [[svn-log-file (svn-csv-options "entity-effort")]
   [git-log-file (git-options "entity-effort")]
   [git2-log-file (git2-options "entity-effort")]
   [p4-log-file (p4-options "entity-effort")]
   [hg-log-file (hg-options "entity-effort")]]
  (is (= (run-with-str-output log-file options)
         "entity,author,author-revs,total-revs\n/Infrastrucure/Network/Connection.cs,APT,1,2\n/Infrastrucure/Network/Connection.cs,XYZ,1,2\n/Presentation/Status/ClientPresenter.cs,APT,1,1\n")))

(def-data-driven-with-vcs-test analysis-of-communication
  [[svn-log-file (svn-csv-options "communication")]
   [git-log-file (git-options "communication")]
   [git2-log-file (git2-options "communication")]
   [p4-log-file (p4-options "communication")]
   [hg-log-file (hg-options "communication")]]
  (is (= (run-with-str-output log-file options)
         "author,peer,shared,average,strength\nXYZ,APT,1,2,50\nAPT,XYZ,1,2,50\n")))

;;; The identity analysis is intended as a debug aid or to
;;; generate parsed VCS data as input to other tools.
;;; The idea with identity is to dump the parse result to
;;; the output.
(deftest svn-identity-analysis-contains-additional-info
  (is (= (run-with-str-output svn-log-file (svn-csv-options "identity"))
         "entity,date,author,action,rev\n/Infrastrucure/Network/Connection.cs,2013-02-08,APT,M,2\n/Presentation/Status/ClientPresenter.cs,2013-02-08,APT,M,2\n/Infrastrucure/Network/Connection.cs,2013-02-07,XYZ,M,1\n")))

;;; The git, Mercurical, and Perforce parsers do not include the
;;; 'action' tag that we have in the current SVN data.
;;; I'm likely to add it later. For now, just document
;;; the behavior here.

(deftest hg-identity-analysis
  (is (= (run-with-str-output hg-log-file (hg-options "identity"))
         "author,rev,date,entity,message\nAPT,2,2013-02-08,/Infrastrucure/Network/Connection.cs,-\nAPT,2,2013-02-08,/Presentation/Status/ClientPresenter.cs,-\nXYZ,1,2013-02-07,/Infrastrucure/Network/Connection.cs,-\n")))

(deftest p4-identity-analysis
  (is (= (run-with-str-output p4-log-file (p4-options "identity"))
         "author,rev,date,entity,message\nAPT,2,2013-02-08,/Infrastrucure/Network/Connection.cs,\nAPT,2,2013-02-08,/Presentation/Status/ClientPresenter.cs,\nXYZ,1,2013-02-07,/Infrastrucure/Network/Connection.cs,\n")))

(deftest git-identity-analysis
  "Git included additional churn info."
  (is (= (run-with-str-output git-log-file (git-options "identity"))
         "author,rev,date,entity,message,loc-added,loc-deleted\nAPT,2,2013-02-08,/Infrastrucure/Network/Connection.cs,git: authors and revisions implemented,1,2\nAPT,2,2013-02-08,/Presentation/Status/ClientPresenter.cs,git: authors and revisions implemented,3,4\nXYZ,1,2013-02-07,/Infrastrucure/Network/Connection.cs,Report connection status,18,2\n")))

;; All age tests are run against a fixed 'now' time specified in the options.
(def-data-driven-with-vcs-test analysis-of-code-age
  [[svn-log-file (svn-csv-options "age")]
   [git-log-file (git-options "age")]
   [git2-log-file (git2-options "age")]
   [p4-log-file (p4-options "age")]
   [hg-log-file (hg-options "age")]]
  (is (= (run-with-str-output log-file options)
         "entity,age-months\n/Infrastrucure/Network/Connection.cs,24\n/Presentation/Status/ClientPresenter.cs,24\n")))

(deftest reports-invalid-arguments
  (testing "Non-existent input file"
    (is (thrown? IllegalArgumentException (app/run "I/do/not/exist"))))
  (testing "Missing mandatory options (normally added by the front)"
    (is (thrown? IllegalArgumentException (app/run svn-log-file {:analysis "coupling"})))))

(def-data-driven-with-vcs-test analysis-of-authors-with-empty-log
  [[empty-log-file (svn-csv-options "authors")]
   [empty-git-file (git-options "authors")]
   [empty-p4-file (p4-options "authors")]
   [empty-hg-file (hg-options "authors")]]
  (is (= (run-with-str-output log-file options)
         "entity,n-authors,n-revs\n")))

(def-data-driven-with-vcs-test analysis-of-coupling-with-empty-log
  [[empty-log-file (svn-csv-options "coupling")]
   [empty-git-file (git-options "coupling")]
   [empty-p4-file (p4-options "coupling")]
   [empty-hg-file (hg-options "coupling")]]
  (is (= (run-with-str-output log-file options)
         "entity,coupled,degree,average-revs\n")))

(def-data-driven-with-vcs-test analysis-of-revisions-with-empty-log
  [[empty-log-file (svn-csv-options "revisions")]
   [empty-git-file (git-options "revisions")]
   [empty-p4-file (p4-options "revisions")]
   [empty-hg-file (hg-options "revisions")]]
  (is (= (run-with-str-output log-file options)
         "entity,n-revs\n")))

(def-data-driven-with-vcs-test analysis-of-effort-with-empty-log
  [[empty-log-file (svn-csv-options "entity-effort")]
   [empty-git-file (git-options "entity-effort")]
   [empty-p4-file (p4-options "entity-effort")]
   [empty-hg-file (hg-options "entity-effort")]]
  (is (= (run-with-str-output log-file options)
         "entity,author,author-revs,total-revs\n")))

(def-data-driven-with-vcs-test analysis-of-communication-with-empty-log
  [[empty-log-file (svn-csv-options "communication")]
   [empty-git-file (git-options "communication")]
   [empty-p4-file (p4-options "communication")]
   [empty-hg-file (hg-options "communication")]]
  (is (= (run-with-str-output log-file options)
         "author,peer,shared,average,strength\n")))

(def-data-driven-with-vcs-test analysis-of-code-age-with-empty-log
  [[empty-log-file (svn-csv-options "age")]
   [empty-git-file (git-options "age")]
   [empty-p4-file (p4-options "age")]
   [empty-hg-file (hg-options "age")]]
  (is (= (run-with-str-output log-file options)
         "entity,age-months\n")))
