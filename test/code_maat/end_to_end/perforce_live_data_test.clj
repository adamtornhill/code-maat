;;; Copyright (C) 2015 Robert Creager
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.end-to-end.perforce-live-data-test
  (:require [code-maat.app.app :as app])
  (:use clojure.test))

(def ^:const p4-log-file "./test/code_maat/end_to_end/sample_p4.log")

(deftest parses-live-data
  (is (= (with-out-str
           (app/run p4-log-file
             {:version-control "p4"
              :analysis "authors"
              :rows 2}))
        "entity,n-authors,n-revs\n/src/something/else/mineral/SweepCmd.cpp,1,1\n/src/something/else/animal/Init.cpp,1,1\n")))
