;;; Copyright (C) 2015 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.end-to-end.git2-live-data-test
  (:require [code-maat.app.app :as app])
  (:use clojure.test))

(def ^:const git-log-file "./test/code_maat/end_to_end/roslyn_git.log")

(deftest parses-live-data
  (is (= (with-out-str
           (app/run git-log-file
                    {:version-control "git2"
                     :analysis "revisions"
                     :rows 2}))
         "entity,n-revs\nsrc/Features/Core/EditAndContinue/ActiveStatementFlags.cs,1\nsrc/EditorFeatures/CSharpTest/EditAndContinue/ActiveStatementTests.cs,1\n")))
