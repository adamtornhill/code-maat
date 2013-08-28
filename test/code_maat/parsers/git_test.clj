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
  "commit 9fa7e32c7457092dbf4b89169d1c24aaf77bb44a
Author: Adam Petersen <adam@adampetersen.se>
Date:   2013-01-30

    Switched to MongoDB persistent sessions in order to scale on several web dynos

 src/cogdrm_web/models/experiment_session.clj |   94 ++++++++++++--------------
 src/cogdrm_web/models/storage.clj            |   24 +++++++
 src/cogdrm_web/server.clj                    |   13 +++-
 test/cogdrm_web/models/storage_test.clj      |    2 +-
 4 files changed, 79 insertions(+), 54 deletions(-)")

(def ^:const entries
  "commit d8ba9b0f53b7d4bec8e7e446f99afeba52c34d06
Author: Adam Petersen <adam@adampetersen.se>
Date:   2013-08-26

    working with complete log

 project.clj                        |    3 +-
 src/git_parse_proto/core.clj       |   31 ++++++++++++------
 test/git_parse_proto/core_test.clj |   62 +++++++++++++++++++++++++++++++++++-
 3 files changed, 85 insertions(+), 11 deletions(-)

commit 3fdfa645e49f26bfed74e667ad6978f5299e00fe
Author: Adam Petersen <adam@adampetersen.se>
Date:   2013-08-24

    Initial prototype, parses one git log entry

 .gitignore                         |   11 +
 README.md                          |   13 ++
 doc/intro.md                       |    3 +
 git_change_stats.txt               |  420 ++++++++++++++++++++++++++++++++++++
 project.clj                        |    7 +
 src/git_parse_proto/core.clj       |   24 +++
 test/git_parse_proto/core_test.clj |   20 ++
 7 files changed, 498 insertions(+)")

(deftest parses-an-entry
  (is (= (git/as-grammar-map entry)
         [[:entry
           [:commit [:hash "9fa7e32c7457092dbf4b89169d1c24aaf77bb44a"]]
           [:author "Adam Petersen <adam@adampetersen.se>"]
           [:date "2013-01-30"]
           [:changes
            [:file "src/cogdrm_web/models/experiment_session.clj"]
            [:file "src/cogdrm_web/models/storage.clj"]
            [:file "src/cogdrm_web/server.clj"]
            [:file "test/cogdrm_web/models/storage_test.clj"]]]])))

(deftest parses-multiple-entries
  (is (= (git/as-grammar-map entries)
         [[:entry
           [:commit [:hash "d8ba9b0f53b7d4bec8e7e446f99afeba52c34d06"]]
           [:author "Adam Petersen <adam@adampetersen.se>"]
           [:date "2013-08-26"]
           [:changes
            [:file "project.clj"]
            [:file "src/git_parse_proto/core.clj"]
            [:file "test/git_parse_proto/core_test.clj"]]]
          [:entry
           [:commit [:hash "3fdfa645e49f26bfed74e667ad6978f5299e00fe"]]
           [:author "Adam Petersen <adam@adampetersen.se>"]
           [:date "2013-08-24"]
           [:changes
            [:file ".gitignore"]
            [:file "README.md"]
            [:file "doc/intro.md"]
            [:file "git_change_stats.txt"]
            [:file "project.clj"]
            [:file "src/git_parse_proto/core.clj"]
            [:file "test/git_parse_proto/core_test.clj"]]]])))

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
           [["Adam Petersen <adam@adampetersen.se>" "9fa7e32c7457092dbf4b89169d1c24aaf77bb44a" "2013-01-30" "src/cogdrm_web/models/experiment_session.clj"]
            ["Adam Petersen <adam@adampetersen.se>" "9fa7e32c7457092dbf4b89169d1c24aaf77bb44a" "2013-01-30" "src/cogdrm_web/models/storage.clj"]
            ["Adam Petersen <adam@adampetersen.se>" "9fa7e32c7457092dbf4b89169d1c24aaf77bb44a" "2013-01-30" "src/cogdrm_web/server.clj"]
            ["Adam Petersen <adam@adampetersen.se>" "9fa7e32c7457092dbf4b89169d1c24aaf77bb44a" "2013-01-30" "test/cogdrm_web/models/storage_test.clj"]]))))