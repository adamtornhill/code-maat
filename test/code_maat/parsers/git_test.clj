;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.git-test
  (:require [code-maat.parsers.git :as git])
  (:use clojure.test incanter.core))

;;; I sure could have used a more minimalistic set of data.
;;; Instead I went with some live data from my projects.
;;; Better ecological validity - we're still just testing
;;; one module though.

(def ^:const entry
  "[990442e] Adam Petersen 2013-08-29 Adapted the grammar after live tests (git)
1	0	project.clj
2	4	src/code_maat/parsers/git.clj
")

(def ^:const binary-entry
  "[990442e] Adam Petersen 2013-11-10 Testing binary entries
-	-	project.bin
2	40	src/code_maat/parsers/git.clj
")

(def ^:const entries
  "[b777738] Adam Petersen 2013-08-29 git: parse merges and reverts too (grammar change)
10	9	src/code_maat/parsers/git.clj
32	0	test/code_maat/parsers/git_test.clj

[a527b79] Adam Petersen 2013-08-29 git: proper error messages from instaparse
6	2	src/code_maat/parsers/git.clj
0	7	test/code_maat/end_to_end/scenario_tests.clj
18	0	test/code_maat/end_to_end/simple_git.txt
21	0	test/code_maat/end_to_end/svn_live_data_test.clj

[a32793d] Ola Flisbäck 2015-09-29 Corrected date of self-awareness to 1997-08-29
1	1	README.md
")

(def ^:const pull-requests
  "[0d3de0c] Mr X 2013-01-04 Merge pull request #1841 from adriaanm/rebase-6827-2.10.x
[77c8751] Mr Y 2013-01-04 SI-6915 Updates copyright properties to 2002-2013
1	1	build.xml
1	1	project/Versions.scala
")

(defn- parse
  [text]
  (git/parse-read-log text {}))

(deftest parses-single-entry-to-dataset
  (is (= (parse entry)
         [{:loc-deleted "0"
           :loc-added "1"
           :author "Adam Petersen"
           :rev "990442e"
           :date "2013-08-29"
           :entity "project.clj"
           :message "Adapted the grammar after live tests (git)"}
          {:loc-deleted "4"
           :loc-added "2"
           :author "Adam Petersen"
           :rev "990442e"
           :date "2013-08-29"
           :entity "src/code_maat/parsers/git.clj"
           :message "Adapted the grammar after live tests (git)"}])))

(deftest parses-entry-with-binary-to-dataset
  "The churn for binary entries are given as a dash (-)."
  (is (= (parse binary-entry)
         [{:loc-deleted "-"
           :loc-added "-"
           :author "Adam Petersen"
           :rev "990442e"
           :date "2013-11-10"
           :entity "project.bin"
           :message "Testing binary entries"}
          {:loc-deleted "40"
           :loc-added "2"
           :author "Adam Petersen"
           :rev "990442e"
           :date "2013-11-10"
           :entity "src/code_maat/parsers/git.clj"
           :message "Testing binary entries"}])))

(deftest parses-multiple-entries-to-dataset
  (is (= (parse entries)
         [{:loc-deleted "9" :loc-added "10"
           :author "Adam Petersen" :rev "b777738" :date "2013-08-29"
           :entity "src/code_maat/parsers/git.clj"
           :message "git: parse merges and reverts too (grammar change)"}
          {:loc-deleted "0" :loc-added "32"
           :author "Adam Petersen" :rev "b777738" :date "2013-08-29"
           :entity "test/code_maat/parsers/git_test.clj"
           :message "git: parse merges and reverts too (grammar change)"}
          {:loc-deleted "2" :loc-added "6"
           :author "Adam Petersen" :rev "a527b79" :date "2013-08-29"
           :entity "src/code_maat/parsers/git.clj"
           :message "git: proper error messages from instaparse"}
          {:loc-deleted "7" :loc-added "0"
           :author "Adam Petersen" :rev "a527b79" :date "2013-08-29"
           :entity "test/code_maat/end_to_end/scenario_tests.clj"
           :message "git: proper error messages from instaparse"}
          {:loc-deleted "0" :loc-added "18",
           :author "Adam Petersen" :rev "a527b79" :date "2013-08-29"
           :entity "test/code_maat/end_to_end/simple_git.txt"
           :message "git: proper error messages from instaparse"}
          {:loc-deleted "0" :loc-added "21"
           :author "Adam Petersen" :rev "a527b79" :date "2013-08-29"
           :entity "test/code_maat/end_to_end/svn_live_data_test.clj"
           :message "git: proper error messages from instaparse"}
          {:loc-deleted "1" :loc-added "1",
           :author "Ola Flisbäck" :rev "a32793d" :date "2015-09-29"
           :entity "README.md"
           :message "Corrected date of self-awareness to 1997-08-29"}])))

(deftest parses-empty-log-to-empty-dataset
  (is (= (parse "")
         [])))

(deftest parses-pull-requests
  "Regression test for a parse bug: there was ambiguity in the grammar and
  we failed to parse a message correctly when it contained a date on the
  same format as the one we expect in the real date field."
  (is (= (parse pull-requests)
         [{:loc-deleted "1"
           :loc-added "1"
           :author "Mr Y"
           :rev "77c8751"
           :date "2013-01-04"
           :entity "build.xml"
           :message "SI-6915 Updates copyright properties to 2002-2013"}
          {:loc-deleted "1"
           :loc-added "1"
           :author "Mr Y"
           :rev "77c8751"
           :date "2013-01-04"
           :entity "project/Versions.scala"
           :message "SI-6915 Updates copyright properties to 2002-2013"}])))

