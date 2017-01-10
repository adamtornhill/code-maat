;;; Copyright (C) 2015 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.end-to-end.git2-live-data-test-with-group
  (:require [code-maat.app.app :as app])
  (:use clojure.test))

(def ^:const git-log-file "./test/code_maat/end_to_end/roslyn_git.log")
(def ^:const text-group-file "./test/code_maat/end_to_end/text-layers-definition.txt")
(def ^:const regex-group-file "./test/code_maat/end_to_end/regex-layers-definition.txt")
(def ^:const regex-and-text-group-file "./test/code_maat/end_to_end/regex-and-text-layers-definition.txt")

(defn- join-lines
  [lines]
  (clojure.string/join "\n" (conj lines "")))

(deftest parses-live-data-with-text-groups
  (is (= (with-out-str
           (app/run git-log-file
                    {:version-control "git2"
                     :analysis "revisions"
                     :group text-group-file
                     }))
         (join-lines
           ["entity,n-revs"
            "Interactive Layer,3"
            "Editor Layer,3"
           ]))))

(deftest parses-live-data-with-regex-groups
  (is (= (with-out-str
           (app/run git-log-file
                    {:version-control "git2"
                     :analysis "revisions"
                     :group regex-group-file
                     }))
         (join-lines
           ["entity,n-revs"
            "Code,7"
            "Unit Tests,4"
           ]))))

(deftest parses-live-data-with-regex-and-text-groups
  (is (= (with-out-str
           (app/run git-log-file
                    {:version-control "git2"
                     :analysis "revisions"
                     :group regex-and-text-group-file
                     }))
         (join-lines
           ["entity,n-revs"
            "Core,4"
            "CS Tests,2"
            "Images,1"
            "VB Tests,1"
           ]))))
