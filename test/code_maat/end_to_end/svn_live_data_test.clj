;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.end-to-end.svn-live-data-test
  (:require [code-maat.app.app :as app]
            [clj-time.core :as clj-time]
            [code-maat.analysis.test-data :as test-data])
  (:use clojure.test))

(def ^:const statsvn-log-file "./test/code_maat/end_to_end/statsvn.log")

(deftest parses-live-data
  (testing "StatSvn: this file has a different format (no kind-attribute on the paths)"
    (is (= (with-out-str
             (app/run statsvn-log-file
                      {:version-control "svn"
                       :analysis "authors"
                       :rows 2}))
           "entity,n-authors,n-revs\n/trunk/statsvn/src/net/sf/statcvs/Main.java,4,19\n/trunk/statsvn/src/net/sf/statcvs/input/Builder.java,4,18\n"))))
