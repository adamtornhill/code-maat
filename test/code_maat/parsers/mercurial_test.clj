;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.mercurial-test
  (:require [code-maat.parsers.mercurial :as hg])
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
  (is (= (hg/parse-log entry {})
         [{:author "apn <apn@somewhere.se>" :rev "47" :date "2010-08-29" :entity ".hgtags"}])))

(deftest parses-multiple-entries-to-dataset
  (is (= (hg/parse-log entries {})
         [{:author "apn" :rev "33" :date "2010-04-14" :entity "impl/CMakeLists.txt"}
          {:author "apn" :rev "33" :date "2010-04-14" :entity "impl/actual_mailbox.cpp"}
          {:author "apn" :rev "33" :date "2010-04-14" :entity "impl/actual_mailbox.h"}
          {:author "xyz" :rev "32" :date "2010-04-03" :entity "impl/node.cpp tinch_pp/node.h"}])))

(deftest parses-empty-log-to-empty-dataset
  (is (= (hg/parse-log "" {})
         [])))
