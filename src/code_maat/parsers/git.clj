;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.git
  (:require [instaparse.core :as insta]))

;;; This module is responsible for parsing a git log file.
;;;
;;; Input: a log file generated with the following command:
;;;         git log --date=short --stat
;;;
;;; Ouput: An incanter dataset with the following columns:
;;;   :entity :date :author :rev
;;; where
;;;  :entity -> the changed entity as a string
;;;  :date -> commit date as a string
;;;  :author -> as a string
;;;  :rev -> the hash used by git to identify the commit
;;;
;;; In the current version we only extract basic info on
;;; authors and file modification patterns.
;;; As we add more analysis options (e.g. churn), it gets
;;; interesting to enable more parse output.

(def ^:const transform-options
  "Specifies parser transformations."
  {:number read-string})

;;; Here's the instaparse grammar for a git log-file:
(def ^:const grammar
  "
    <S> = entries
    <entries> = (entry <nl*>)* | entry
    entry = commit <nl> author <nl> date <nl> <message> <nl> changes
    commit = <'commit'> <ws> hash
    author = <'Author:'> <ws> #'.+'
    date = <'Date:'> <ws> #'.+'
    message = <nl> <ws> #'.+' <nl>
    changes = change* <summary>
    <change> = <ws*> file <ws> <'|'> <ws> <modification> <nl>
    file = #'[^\\s]+'
    modification = lines_modified <ws> modification_churn
    lines_modified = number
    modification_churn = #'[\\+\\-]+'
    summary = files_changed? <ws*> insertions? <ws*> deletions?
    files_changed = <ws*> number <ws> <files_changed_static>
    files_changed_static = 'file' 's'? ' changed,'
    insertions = number <ws> <'insertion'  's'? '(+)'><','?>
    deletions = number <ws> <'deletion' 's'? '(-)'>
    number = #'\\d+'
    ws = #'\\s+'
    nl = '\\n'
    hash = #'[\\da-f]+'")

(def git-log-parser
  (insta/parser grammar))

(defn parse
  [input]
  (->>
   (git-log-parser input)
   (insta/transform transform-options)))

