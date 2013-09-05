;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.mercurial-test
  (:require [code-maat.parsers.mercurial :as hg]
            [incanter.core :as incanter])
  (:use clojure.test incanter.core))

(def ^:const entry
  "rev: 47 author: apn <apn@somewhere.se> date: 2010-08-29 files:
.hgtags")

(def ^:const entries
  "rev: 33 author: apn date: 2010-04-14 files:
impl/CMakeLists.txt
impl/actual_mailbox.cpp
impl/actual_mailbox.h

rev: 32 author: xyz date: 2010-04-03 files:
impl/node.cpp tinch_pp/node.h")

(deftest parses-single-entry-to-dataset
  (is (= (incanter/to-list (hg/parse-log entry {}))
         [["apn <apn@somewhere.se>" "47" "2010-08-29" ".hgtags"]])))

(deftest parses-multiple-entries-to-dataset
  (is (= (incanter/to-list (hg/parse-log entries {}))
         [["apn" "33" "2010-04-14" "impl/CMakeLists.txt"]
          ["apn" "33" "2010-04-14" "impl/actual_mailbox.cpp"]
          ["apn" "33" "2010-04-14" "impl/actual_mailbox.h"]
          ["xyz" "32" "2010-04-03" "impl/node.cpp tinch_pp/node.h"]])))

(deftest parses-empty-log-to-empty-dataset
  (is (= (incanter/to-list (hg/parse-log "" {}))
         [])))