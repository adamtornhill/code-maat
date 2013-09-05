;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.git-test
  (:require [code-maat.parsers.git :as git]
            [incanter.core :as incanter])
  (:use clojure.test incanter.core))

;;; I sure could have used a more minimalistic set of data.
;;; Instead I went with some live data from my projects.
;;; Better ecological validity - we're still just testing
;;; one module though.

(def ^:const entry
  "[990442e] Adam Petersen 2013-08-29 Adapted the grammar after live tests (git)
M	project.clj
M	src/code_maat/parsers/git.clj
")

(def ^:const entries
  "[b777738] Adam Petersen 2013-08-29 git: parse merges and reverts too (grammar change)
M	src/code_maat/parsers/git.clj
M	test/code_maat/parsers/git_test.clj

[a527b79] Adam Petersen 2013-08-29 git: proper error messages from instaparse
M	src/code_maat/parsers/git.clj
M	test/code_maat/end_to_end/scenario_tests.clj
A	test/code_maat/end_to_end/simple_git.txt
A	test/code_maat/end_to_end/svn_live_data_test.clj
")

(deftest throws-on-invalid-input
  (is (thrown? IllegalArgumentException
               (git/parse-log "simply not a valid git log here..." {}))))

(deftest parses-single-entry-to-dataset
  (is (= (incanter/to-list (git/parse-log entry {}))
         [["Adam Petersen" "990442e" "2013-08-29" "project.clj"]
          ["Adam Petersen" "990442e" "2013-08-29" "src/code_maat/parsers/git.clj"]])))

(deftest parses-multiple-entries-to-dataset
  (is (= (incanter/to-list (git/parse-log entries {}))
         [["Adam Petersen" "b777738" "2013-08-29" "src/code_maat/parsers/git.clj"]
          ["Adam Petersen" "b777738" "2013-08-29" "test/code_maat/parsers/git_test.clj"]
          ["Adam Petersen" "a527b79" "2013-08-29" "src/code_maat/parsers/git.clj"]
          ["Adam Petersen" "a527b79" "2013-08-29" "test/code_maat/end_to_end/scenario_tests.clj"]
          ["Adam Petersen" "a527b79" "2013-08-29" "test/code_maat/end_to_end/simple_git.txt"]
          ["Adam Petersen" "a527b79" "2013-08-29" "test/code_maat/end_to_end/svn_live_data_test.clj"]])))

(deftest parses-empty-log-to-empty-dataset
  (is (= (incanter/to-list (git/parse-log "" {}))
         [])))