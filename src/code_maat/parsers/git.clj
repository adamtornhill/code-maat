;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.git
  (:require [instaparse.core :as insta]
            [code-maat.parsers.time-parser :as tp]
            [code-maat.parsers.hiccup-based-parser :as hbp]))

;;; This module is responsible for parsing a git log file.
;;;
;;; Input: a log file generated with the following command:
;;;         
;;;    git log --pretty=format:'[%h] %an %ad %s' --date=short --numstat
;;;
;;; Ouput: A sequence of maps where each map represents a change entry
;;;        from the version-control log:
;;;   :entity :date :author :rev
;;; where
;;;  :entity -> the changed entity as a string
;;;  :date -> commit date as a string
;;;  :author -> as a string
;;;  :rev -> the hash used by git to identify the commit

(def ^:const git-grammar
  "Here's the instaparse grammar for a git entry.
   Note that we parse the entries one by one (Instaparse memory optimization).

   In the current version we only extract basic info on
   authors and file modification patterns.
   To calculate churn, we parse the lines added/deleted too.
   That info is added b the numstat argument."
  "
    entry     = <prelude*> prelude changes (* covers pull requests *)
    <prelude> = rev <ws> author <ws> date <ws> message <nl>
    rev       =  <'['> #'[\\da-f]+' <']'>
    author    =  #'.+?(?=\\s\\d{4}-\\d{2}-\\d{2})' (* match until the date field *)
    date      =  #'\\d{4}-\\d{2}-\\d{2}'
    message   =  #'[^\\n]*'
    changes   =  change*
    change    =  added <tab> deleted <tab> file <nl>
    added     =  numstat
    deleted   =  numstat
    <numstat> =  #'[\\d-]*' (* binary files are presented with a dash *)
    file      =  #'.+'
    ws        =  #'\\s'
    tab       =  #'\\t'
    nl        =  '\\n'")

(def as-common-time-format (tp/time-string-converter-from "YYYY-MM-dd"))

(def positional-extractors
  "Specify a set of functions to extract the parsed values."
  {:rev #(get-in % [1 1])
   :author #(get-in % [2 1])
   :date #(as-common-time-format (get-in % [3 1]))
   :message #(get-in % [4 1])
   :changes #(rest (get-in % [5]))})

(defn parse-log
  "Transforms the given input git log into an
   Incanter dataset suitable for the analysis modules." 
  [input-file-name options]
  (hbp/parse-log input-file-name
                 options
                 git-grammar
                 positional-extractors))

(defn parse-read-log
  [input-text options]
  (hbp/parse-read-log input-text
                      options
                      git-grammar
                      positional-extractors))
