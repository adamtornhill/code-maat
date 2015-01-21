;;; Copyright (C) 2015 Robert Creager
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns
  code-maat.parsers.perforce-test
  (:require [code-maat.parsers.perforce :as p4])
  (:use clojure.test incanter.core))

(def ^:const entry
  "Change 1108116 by user1@client on 2014/12/19 14:40:17

	Fix Stuff.
	       More comments

Affected files ...

... //depot/project/Makefile#3 edit
")

(def ^:const entries
  "Change 1108116 by user1@client on 2014/12/19 14:40:17

	Fix Stuff.
	       More comments

Affected files ...

... //depot/project/Makefile#3 edit

Change 1108117 by user2@client on 2014/12/19 15:41:18

	Fix More Stuff.
	       More comments

Affected files ...

... //depot/project/meta/recipes-core/udev/udev-extraconf/mount.blacklist#2 edit
")

(deftest parses-single-entry-to-dataset
  (is (= (p4/parse-log entry {})
        [{:author "user1"
          :rev "1108116"
          :date "2014/12/19"
          :entity "/Makefile"
          :message ""}
         ])))

(deftest parses-multiple-entries-to-dataset
  (is (= (p4/parse-log entries {})
        [{:author "user1"
          :rev "1108116"
          :date "2014/12/19"
          :entity "/Makefile"
          :message ""}
         {:author "user2"
          :rev "1108117"
          :date "2014/12/19"
          :entity "/meta/recipes-core/udev/udev-extraconf/mount.blacklist"
          :message ""}
         ])))

(deftest throws-on-invalid-input
  (is (thrown? IllegalArgumentException
        (p4/parse-log "simply not a valid perforce log here..." {}))))

(deftest parses-empty-log-to-empty-dataset
  (is (= (p4/parse-log "" {})
        [])))
