;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.mercurial
  (:require [code-maat.parsers.hiccup-based-parser :as hbp]
            [code-maat.parsers.time-parser :as tp]))

;;; This module is responsible for parsing a Mercurial log file.
;;;
;;; Input: a log file generated with the following command:
;;;         
;;;    hg log --template "rev: {rev} author: {author} date: {date|shortdate} files:\n{files %'{file}\n'}\n"
;;;
;;; The command above uses Mercurial's templating system to format an
;;; output with each file in the changeset separated by newlines.
;;;
;;; Ouput: An sequence of maps where each map represents a change
;;;        entry with the following keyes:
;;;   :entity :date :author :rev
;;; where
;;;  :entity -> the changed entity as a string
;;;  :date -> commit date as a string
;;;  :author -> as a string
;;;  :rev -> revision from Mercurial

(def ^:const hg-grammar
  "Here's the instaparse grammar for a Mercurial log entry.
   Note that we parse the entries one by one (Instaparse memory optimization)."
   "
    entry     =  rev <ws> author <ws> date <ws> changes
    rev       =  <'rev: '> #'\\d+'
    author    =  <'author: '> #'.+(?=\\sdate:\\s\\d{4}-)' (* match until the date field *)
    date      =  <'date: '> #'\\d{4}-\\d{2}-\\d{2}'
    changes   =  <'files:'> <nl> (file <nl?>)*
    file      =  #'.+'
    ws        =  #'\\s'
    nl        =  '\\n'
    ")

(def as-common-time-format (tp/time-string-converter-from "YYYY-MM-dd"))

(def positional-extractors
  "Specify a set of functions to extract the parsed values."
  {:rev #(get-in % [1 1])
   :author #(get-in % [2 1])
   :date #(as-common-time-format (get-in % [3 1]))
   :message (fn [_] "-")
   :changes #(rest (get-in % [4]))})

(defn parse-log
  "Transforms the given input MErcurial log into an
   Incanter dataset suitable for the analysis modules." 
  [input-file-name options]
  (hbp/parse-log input-file-name options hg-grammar positional-extractors))

(defn parse-read-log
  [input-text options]
  (hbp/parse-read-log input-text
                      options
                      hg-grammar
                      positional-extractors))
