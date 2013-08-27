;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.git
  (:require [instaparse.core :as insta]
            [incanter.core :as incanter]
            [clojure.zip :as z]
            [clojure.data.zip :as dz]))

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

(defn as-grammar-map
  [input]
   (git-log-parser input))

;;; The parse result is fed into a zipper.
;;; The functions below are accessors to different parts of the
;;; information stored in the zipper.
;;; Note that we never expose the zipper; it's an implementation detail.

(defn- entries [z]
  "Returns the top-level elements from the zipper as
   a lazy seq."
  (map #(-> % z/down) (dz/children z)))

(defn- rev [z]
  (-> z z/right z/down z/right z/down z/right z/node))

(defn- author [z]
  (-> z z/right z/right z/down z/right z/node))

(defn- date [z]
  (-> z z/right z/right z/right z/down z/right z/node))

(defn- changes [z]
  (rest
   (map #(-> % z/node)
       (-> z  z/right z/right z/right z/right dz/children))))

(defn- files [z]
  (map (fn [[tag name]] name) (changes z)))

(defn- entry-as-row
  [coll z]
  (let [author (author z)
        rev (rev z)
        date (date z)
        files (files z)]
    (reduce conj
            coll
            (for [file files]
              {:author author :rev rev :date date :file file}))))

(defn grammar-map->rows
  "Transforms the parse result (our grammar map) into
   a seq of maps where each map represents one entity.
   The grammar map is given as nested hiccup vectors."
  [gm]
  (->>
   gm
   z/vector-zip
   entries
   (reduce entry-as-row []))) 

(defn parse-log
  [input]
  (->
   as-grammar-map
   grammar-map->rows
   incanter/to-dataset))
