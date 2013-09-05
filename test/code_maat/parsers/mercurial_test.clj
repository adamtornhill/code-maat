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

(deftest parses-an-entry
  (is (= (hg/as-grammar-map entry)
         [[:entry [:rev "47"]
           [:author "apn <apn@somewhere.se>"]
           [:date "2010-08-29"]
           [:changes [:file ".hgtags"]]]])))

(deftest parses-multiple-entries
  (is (= (hg/as-grammar-map entries)
         [[:entry [:rev "33"]
           [:author "apn"]
           [:date "2010-04-14"]
           [:changes
            [:file "impl/CMakeLists.txt"]
            [:file "impl/actual_mailbox.cpp"]
            [:file "impl/actual_mailbox.h"]]]
          [:entry [:rev "32"]
           [:author "xyz"]
           [:date "2010-04-03"]
           [:changes [:file "impl/node.cpp tinch_pp/node.h"]]]])))