;;; Copyright (C) 2015 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.git2
  (:require [instaparse.core :as insta]
            [code-maat.parsers.time-parser :as tp]
            [code-maat.parsers.hiccup-based-parser :as hbp]))

;;; This module is responsible for parsing a git log file.
;;;
;;; NOTE: this is the prefered git parser - we have one more that 
;;; exists for backwards compatibility, but the git2 parser is 
;;; more tolerant and faster.
;;;
;;; Input: a log file generated with the following command:
;;;         
;;;    git log --all -M -C --numstat --date=short --pretty=format:'--%h--%cd--%cn'
;;;
;;; Ouput: A sequence of maps where each map represents a change entry
;;;        from the version-control log:
;;;   :entity :date :author :rev
;;; where
;;;  :entity -> the changed entity as a string
;;;  :date -> commit date as a string
;;;  :author -> as a string
;;;  :rev -> the hash used by git to identify the commit

;;; Sample input where each commit is separated by a whitespace:
(comment
  --586b4eb--2015-06-15--Adam Tornhill
  35      0       src/code_maat/mining/vcs.clj)

(def ^:const git-grammar
  "Here's the instaparse grammar for a git entry.
   Note that we parse the entries one by one (Instaparse memory optimization).

   In the current version we only extract basic info on
   authors and file modification patterns.
   To calculate churn, we parse the lines added/deleted too.
   That info is added b the numstat argument."
  "
    entry     = <prelude*> prelude changes (* covers pull requests *)
    <prelude> = <separator> rev <separator> date <separator> author <nl>
    rev       =  #'[\\da-f]+'
    author    =  #'[^\\n]*'
    date      =  #'\\d{4}-\\d{2}-\\d{2}'
    changes   =  change*
    change    =  added <tab> deleted <tab> file <nl>
    added     =  numstat
    deleted   =  numstat
    <numstat> =  #'[\\d-]*' (* binary files are presented with a dash *)
    file      =  #'.+'
    separator = '--'
    ws        =  #'\\s'
    tab       =  #'\\t'
    nl        =  '\\n'")

(def as-common-time-format (tp/time-string-converter-from "YYYY-MM-dd"))

(def positional-extractors
  "Specify a set of functions to extract the parsed values."
  {:rev #(get-in % [1 1])
   :date #(as-common-time-format (get-in % [2 1]))
   :author #(get-in % [3 1])
   :changes #(rest (get-in % [4]))
   :message (fn [_] "-")}) ; NOTE: use the git legacy parser to extract commit messages

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
