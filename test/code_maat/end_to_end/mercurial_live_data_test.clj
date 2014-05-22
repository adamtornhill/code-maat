;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.end-to-end.mercurial-live-data-test
  (:require [code-maat.app.app :as app])
  (:use clojure.test))

(def ^:const hg-log-file "./test/code_maat/end_to_end/tpp_hg.log")

(deftest parses-live-data
  (is (= (with-out-str
           (app/run hg-log-file
                    {:version-control "hg"
                     :analysis "authors"
                     :rows 2}))
         "entity,n-authors,n-revs\ntest/CMakeLists.txt,3,21\nCMakeLists.txt,3,6\n")))
