;;; Copyright (C) 2015 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.git2-test
  (:require [code-maat.parsers.git2 :as git])
  (:use clojure.test incanter.core))

(def ^:const entry
  "--990442e--2013-08-29--Adam Petersen
1	0	project.clj
2	4	src/code_maat/parsers/git.clj
")

(def ^:const binary-entry
  "--990442e--2013-11-10--Adam Petersen
-	-	project.bin
2	40	src/code_maat/parsers/git.clj
")

(def ^:const entries
  "--b777738--2013-08-29--Adam Petersen
10	9	src/code_maat/parsers/git.clj
32	0	test/code_maat/parsers/git_test.clj

--a527b79--2013-08-29--Adam Petersen
6	2	src/code_maat/parsers/git.clj
0	7	test/code_maat/end_to_end/scenario_tests.clj
18	0	test/code_maat/end_to_end/simple_git.txt
21	0	test/code_maat/end_to_end/svn_live_data_test.clj
")

(def ^:const pull-requests
  "--0d3de0c--2013-01-04--Mr X
--77c8751--2013-01-04--Mr Y
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
           :message "-"}
          {:loc-deleted "4"
           :loc-added "2"
           :author "Adam Petersen"
           :rev "990442e"
           :date "2013-08-29"
           :entity "src/code_maat/parsers/git.clj"
           :message "-"}])))

(deftest parses-entry-with-binary-to-dataset
  "The churn for binary entries are given as a dash (-)."
  (is (= (parse binary-entry)
         [{:loc-deleted "-"
           :loc-added "-"
           :author "Adam Petersen"
           :rev "990442e"
           :date "2013-11-10"
           :entity "project.bin"
           :message "-"}
          {:loc-deleted "40"
           :loc-added "2"
           :author "Adam Petersen"
           :rev "990442e"
           :date "2013-11-10"
           :entity "src/code_maat/parsers/git.clj"
           :message "-"}])))

(deftest parses-multiple-entries-to-dataset
  (is (= (parse entries)
         [{:loc-deleted "9" :loc-added "10"
           :author "Adam Petersen" :rev "b777738" :date "2013-08-29"
           :entity "src/code_maat/parsers/git.clj"
           :message "-"}
          {:loc-deleted "0" :loc-added "32"
           :author "Adam Petersen" :rev "b777738" :date "2013-08-29"
           :entity "test/code_maat/parsers/git_test.clj"
           :message "-"}
          {:loc-deleted "2" :loc-added "6"
           :author "Adam Petersen" :rev "a527b79" :date "2013-08-29"
           :entity "src/code_maat/parsers/git.clj"
           :message "-"}
          {:loc-deleted "7" :loc-added "0"
           :author "Adam Petersen" :rev "a527b79" :date "2013-08-29"
           :entity "test/code_maat/end_to_end/scenario_tests.clj"
           :message "-"}
          {:loc-deleted "0" :loc-added "18",
           :author "Adam Petersen" :rev "a527b79" :date "2013-08-29"
           :entity "test/code_maat/end_to_end/simple_git.txt"
           :message "-"}
          {:loc-deleted "0" :loc-added "21"
           :author "Adam Petersen" :rev "a527b79" :date "2013-08-29"
           :entity "test/code_maat/end_to_end/svn_live_data_test.clj"
           :message "-"}])))

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
           :message "-"}
          {:loc-deleted "1"
           :loc-added "1"
           :author "Mr Y"
           :rev "77c8751"
           :date "2013-01-04"
           :entity "project/Versions.scala"
           :message "-"}])))
