;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.limitters
  (:require [clj-time.core :as clj-time]
            [clj-time.format :as time-format]))

;;; The input from a VCS log is filtered (optionally) on both
;;; the total number of entries considered as well as a specified
;;; date span. The filtering is encapsulated within this module and
;;; shared between the different front-end parsers (e.g. git, svn).

(defn make-entries-limited-seq [parse-options s]
  (if-let [n (:max-entries parse-options)]
    (take n s)
    s))

(defn- after-start-date?
  "A predicate that returns true if the given log-entry contains
   a time span after the start-time.
   The intent is to limit the commits included in the analysis.
   Over time, design issues get fixed and we don't want old
   data to interfere with our analysis results."
  [start-date entity-date-extractor-fn log-entry]
  (clj-time/after? (entity-date-extractor-fn log-entry) start-date))

(defn make-date-span-limited-seq
  [parse-options entity-date-extractor-fn s]
  (if-let [date (:date parse-options)]
    (take-while
     (partial after-start-date? date entity-date-extractor-fn)
     s)
    s))

(defn log-entries-to-include
  "Returns a lazy seq of s filtered and limited according
   to the given parse-options. The third argument is simply
   a function that given an entry returns its date as a clj-time
   instance."
  [parse-options entity-date-extractor-fn s]
  (let [limitter (comp
                  (partial make-date-span-limited-seq
                           parse-options
                           entity-date-extractor-fn)
                  make-entries-limited-seq)]
  (limitter parse-options s)))