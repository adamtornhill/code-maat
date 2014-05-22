;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.end-to-end.git-live-data-test
  (:require [code-maat.app.app :as app])
  (:use clojure.test))

(def ^:const git-log-file "./test/code_maat/end_to_end/mono_git.log")

(deftest parses-live-data
  (is (= (with-out-str
           (app/run git-log-file
                    {:version-control "git"
                     :analysis "authors"
                     :rows 2}))
         "entity,n-authors,n-revs\nmono/mini/method-to-ir.c,2,5\nmcs/class/corlib/System/Console.iOS.cs,2,3\n")))
