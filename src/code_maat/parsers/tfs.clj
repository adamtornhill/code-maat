;;; Copyright (C) 2016 Ryan Coy
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.tfs
	(:require [instaparse.core :as insta]
        [clojure.string :as str]
			  [code-maat.parsers.time-parser :as tp]
			  [code-maat.parsers.hiccup-based-parser :as hbp]))

;;; This module is responsible for parsing a TFS log file.
;;;
;;; Input: a log file generated with the following command:
;;; 
;;; 	tf hist {path} /noprompt /format:detailed /recursive
;;;
;;; Output: A sequence of maps where each map represents a change entry
;;;			from the version-control log:
;;;		:entity :date :author :rev
;;; where
;;;	:entity -> the changed entity as a string
;;; :date 	-> changeset date as a string
;;;	:author -> as a string
;;; :rev 	-> the changeset version from TFS

;;; Sample input where each commit is separated by the dividing line
;;;
;;;	-----------------------------------------------------------------------------------------------------------------------
;;;	Changeset: 1
;;;	User: Ryan Coy
;;;	Date: Thursday, March 17, 2016 12:44:13 PM
;;;
;;;	Comment:
;;;		Created team project folder $/MyProject via the Team Project Creation Wizard
;;;
;;;	Items:
;;;		add $/MyProject

(def ^:const tfs-grammar
	"This is the instaparse grammar for a TFS entry.
	 Things get parsed one-by-one for memory optimization
	 TFS doesn't give us lines added/deleted, so we only get the core metrics"
	"
    changeset     = <sep> changelog userinfo timestamp message changes <nl*>
    sep           = '-'* <nl>
    <changelog>   = <'Changeset: '> id <nl>
    id            = #'[\\d]+'
    <userinfo>    = <'User: '> author <nl>
    author        = #'.+'
    <timestamp>   = <'Date: '> date <nl>
    date          = #'\\w+day, \\w+ \\d+, \\d{4} \\d+:\\d+:\\d+ [AP]M'
    message       = <'Comment:'> <nl> <'  '> #'[\\S ]+' <nl>
    changes       = <'Items:'> <nl> file*
    file          = <ws+> <action+> <'$'> #'.+' <nl?>
    action        = #'[a-zA-Z, ]+'
    ws            = #'\\s'
    nl            = '\\r'?'\\n'")

;;; TFS logs come with whitespace inside individual entries
;;; We remove this because the main parser uses blank lines
;;; to identify the break between entries

(defn tfs-preparse
  "Removes whitespace lines inside individual changeset log entries
   char-array is used as a java.io.reader compatible input since we won't
   be giving it an actual file"
  [input-file-text]
  (char-array  (str/replace  input-file-text
                             #"\r?\n\r?\n(?!-)"
                             "\r\n")))

;;; TFS sample: Monday, November 2, 2015 8:16:41 AM
(def as-common-time-format (tp/time-string-converter-from "E, MMMM d, yyyy H:m:s a"))

(def positional-extractors
	"Specify a set of functions to extract the parsed values."
  {:rev #(get-in % [1 1])
   :date #(as-common-time-format (get-in % [3 1]))
   :author #(get-in % [2 1])
	 :changes #(rest (get-in % [5]))
	 :message #(get-in % [4 1])})

(defn parse-log
	"Transforms the given input TFS log into an 
	 Incanter dataset suitable for the analysis modules."
	 [input-file-name options]
	 (hbp/parse-log (tfs-preparse (slurp input-file-name))
                  options
                  tfs-grammar
                  positional-extractors))

(defn parse-read-log
	[input-text options]
	(hbp/parse-read-log (clojure.string/join (tfs-preparse input-text))
                      options
                      tfs-grammar
                      positional-extractors))

