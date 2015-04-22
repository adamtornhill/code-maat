;;; Copyright (C) 2015 Robert Creager
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.perforce
  (:require [instaparse.core :as insta]
            [code-maat.parsers.time-parser :as tp]
            [code-maat.parsers.hiccup-based-parser :as hbp]))

;;; This module is responsible for parsing a perforce log file.
;;;
;;; Input: a log file generated with the following command:
;;;
;;;    p4 changes -s submitted -m 5000 //depot/project/... | cut -d ' ' -f 2 | xargs -I commitid -n1 sh -c 'p4 describe -s commitid | grep -v "^\s*$" && echo ""'
;;;
;;; Ouput: An sequence of maps where each map represents a change
;;;        entry with the following keyes:
;;;   :entity :date :author :rev
;;; where
;;;  :entity -> the changed entity as a string
;;;  :date -> commit date as a string
;;;  :author -> as a string
;;;  :rev -> the commit id from perforce

(def ^:const perforce-grammar
  "Here's the instaparse grammar for a perforce change descriptions.
   In the current version we only extract basic info on
   authors and file modification patterns.
   Note that we parse the entries one by one (Instaparse memory optimization)."
  "
   entry     =  rev <ws> author <ws> date <nl> <message> <header> changes
   rev       =  <'Change' ws> #'[\\d]+'
   author    =  <'by' ws> #'[^@]+' <#'[^\\s]+'>
   date      =  <'on' ws> #'\\d{4}/\\d{2}/\\d{2}' <ws #'\\d{2}:\\d{2}:\\d{2}'>
   message   =  (ws* #'.+' nl)+
   header    =  'Affected files ...' nl
   changes   =  (file <nl>)+
   file      =  <'... //' #'[^/]+/' #'[^/]+'> #'[^#]+' <#'.+'>
   ws        =  #'\\s'
   nl        =  #'(\\r)?\\n'
   ")

(def as-common-time-format (tp/time-string-converter-from "YYYY/MM/dd"))

(def positional-extractors
  "Specify a set of functions to extract the parsed values."
  {:rev #(get-in % [1 1])
   :author #(get-in % [2 1])
   :date #(as-common-time-format (get-in % [3 1]))
   :message (fn [_] "")
   :changes #(rest (get-in % [4]))
   })

(defn parse-log
  "Transforms the given input perforce log into an
   Incanter dataset suitable for the analysis modules."
  [input-file-name options]
  (hbp/parse-log
    input-file-name
    options
    perforce-grammar
    positional-extractors))

(defn parse-read-log
  [input-text options]
  (hbp/parse-read-log
    input-text
    options
    perforce-grammar
    positional-extractors))
