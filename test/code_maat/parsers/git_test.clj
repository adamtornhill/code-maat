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
  (is (= (git/parse entry)
         [[:ENTRY
           [:author "Adam Petersen <adam@adampetersen.se>"]
           [:date "2013-01-30"]
           [:message "Switched to MongoDB persistent sessions in order to scale on several web dynos"]
           [:changes
            [:change
             [:file "src/cogdrm_web/models/experiment_session.clj"]]
            [:change
             [:file "src/cogdrm_web/models/storage.clj"]]
            [:change
             [:file "src/cogdrm_web/server.clj"]]
            [:change
             [:file "test/cogdrm_web/models/storage_test.clj"]]]]])))


(deftest parses-multiple-entries
  (is (= (git/parse entries)
         [[:ENTRY
           [:author "Adam Petersen <adam@adampetersen.se>"]
           [:date "2013-08-26"]
           [:message "working with complete log"]
           [:changes
            [:change [:file "project.clj"]]
            [:change [:file "src/git_parse_proto/core.clj"]]
            [:change [:file "test/git_parse_proto/core_test.clj"]]]]
          [:ENTRY
           [:author "Adam Petersen <adam@adampetersen.se>"]
           [:date "2013-08-24"]
           [:message "Initial prototype, parses one git log entry"]
           [:changes
            [:change [:file ".gitignore"]]
            [:change [:file "README.md"]]
            [:change [:file "doc/intro.md"]]
            [:change [:file "git_change_stats.txt"]]
            [:change [:file "project.clj"]]
            [:change [:file "src/git_parse_proto/core.clj"]]
            [:change [:file "test/git_parse_proto/core_test.clj"]]]]])))

(deftest parses-empty-log
  (is (= (git/parse "")
         [])))