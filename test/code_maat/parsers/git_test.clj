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

(deftest parses-an-entry
  (is (= (git/as-grammar-map entry)
         [[:entry
           [:commit [:hash "990442e"]]
           [:author "Adam Petersen"]
           [:date "2013-08-29"]
           [:changes
            [:file "project.clj"]
            [:file "src/code_maat/parsers/git.clj"]]]])))

(deftest parses-multiple-entries
  (is (= (git/as-grammar-map entries)
         [[:entry
           [:commit [:hash "b777738"]]
           [:author "Adam Petersen"]
           [:date "2013-08-29"]
           [:changes
            [:file "src/code_maat/parsers/git.clj"]
            [:file "test/code_maat/parsers/git_test.clj"]]]
          [:entry
           [:commit [:hash "a527b79"]]
           [:author "Adam Petersen"]
           [:date "2013-08-29"]
           [:changes
            [:file "src/code_maat/parsers/git.clj"]
            [:file "test/code_maat/end_to_end/scenario_tests.clj"]
            [:file "test/code_maat/end_to_end/simple_git.txt"]
            [:file "test/code_maat/end_to_end/svn_live_data_test.clj"]]]])))

(deftest parses-empty-log
  (is (= (git/as-grammar-map "")
         [])))

(deftest throws-on-invalid-input
  (is (thrown? IllegalArgumentException
               (git/as-grammar-map "simply not a valid git log here..."))))

(deftest transforms-parse-result-to-rows-for-dataset
  (is (= (git/grammar-map->rows
           [[:entry
           [:commit [:hash "123"]]
           [:author "a"]
           [:date "2013-01-30"]
           [:changes
            [:file "first.clj"]
            [:file "second.clj"]]]
            [:entry
             [:commit [:hash "456"]]
           [:author "b"]
           [:date "2013-10-30"]
           [:changes
            [:file "third.clj"]]]])
         [{:author "a", :rev "123", :date "2013-01-30", :entity "first.clj"}
          {:author "a", :rev "123", :date "2013-01-30", :entity "second.clj"}
          {:author "b", :rev "456", :date "2013-10-30", :entity "third.clj"}])))

(deftest parses-to-dataset
  (testing "single entry in log"
    (is (= (incanter/to-list (git/parse-log entry {}))
           [["Adam Petersen" "990442e" "2013-08-29" "project.clj"]
            ["Adam Petersen" "990442e" "2013-08-29" "src/code_maat/parsers/git.clj"]]))))