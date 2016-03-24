;;; Copyright (C) 2016 Ryan Coy
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.end-to-end.tfs-live-data-test
  (:require [code-maat.app.app :as app])
  (:use clojure.test))

(def ^:const tfs-log-file "./test/code_maat/end_to_end/tfs.log")

(deftest parses-live-data
  (is (= (with-out-str
           (app/run tfs-log-file
                    {:version-control "tfs"
                     :analysis "revisions"
                     :rows 2}))
         "entity,n-revs\n/Project/Project/Project/View/MainWindow.xaml,2\n/Project/Project/Project/ViewModel/MainViewModel.cs,2\n")))
