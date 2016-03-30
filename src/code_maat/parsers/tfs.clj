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
    changeset     = <sep> changelog userinfo <proxy?> timestamp comment changes <nl*>
    sep           = '-'* <nl>
    <changelog>   = <'Changeset: '> id <nl>
    id            = #'[\\d]+'
    <userinfo>    = <'User: '> author <nl>
    author        = #'.+'
    <proxy>       = <'Checked in by: '> #'.+' <nl>
    <timestamp>   = <'Date: '> date <nl>
    date          = #'.+'
    <comment>     = <'Comment:'> <nl> message
    message       = line*
    <line>        = <'  '> #'[\\S ]+' <nl>
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

;;; This matches the default EN-US format:
;;; Wednesday, March 23, 2016 7:34:43 PM
(def as-common-time-format (tp/time-string-converter-from "E, MMMM d, yyyy H:m:s a"))

;;; The date parsing attempts to parse some common formats
(def positional-extractors
	"Specify a set of functions to extract the parsed values."
  {:rev     #(get-in % [1 1])
   :date    (fn [entry] (let [date-string (get-in entry [3 1])]
                (try
                  (as-common-time-format date-string)
                  (catch Exception e (throw (IllegalArgumentException. (str "Unsupported TFS Date Format: " date-string)))))))
   :author  #(get-in % [2 1])
   :changes #(rest (get-in % [5]))
   :message (fn [entry] (let [message (get-in entry [4])]
                          (str/join "\n" (rest message))))})

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