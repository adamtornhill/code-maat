;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.hiccup-based-parser
  (:require [instaparse.core :as insta]
            [incanter.core :as incanter]))

;;; This module encapsulates the common functionality of parsing a
;;; VCS log into Hiccup format using Instaparse.
;;; Clients parameterize this module with the actual grammar (e.g. git or hg).

(defn- raise-parse-failure
  [f]
  (let [reason (with-out-str (print f))]
    (throw (IllegalArgumentException. reason))))

(defn as-grammar-map
  "The actual invokation of the parser.
   Returns a Hiccup parse tree upon success,
   otherwise an informative exception is thrown."
  [parser input]
  (let [result (insta/parse parser input)]
    (if (insta/failure? result)
      (raise-parse-failure (insta/get-failure result))
      result)))

;;; The parse result from instaparse is given as hiccup vectors.
;;; We define a set of accessors encapsulating the access to
;;; the individual parts of the associative vectors.
;;; Example input: a seq of =>
;;; [:entry
;;;  [:rev "123"]
;;;  [:author "a"]
;;;  [:date "2013-01-30"]
;;;  [:changes
;;;   [:file ...]]]

(defn- rev [z]
  (get-in z [1 1]))

(defn- author [z]
  (get-in z [2 1]))

(defn- date [z]
  (get-in z [3 1]))

(defn- changes [z]
  "The parsed changes includes info on the number of
   lines added, deleted and the name of the entity itself.
   For example:
    [:changes [:added 1] [:deleted 1] [:file project.clj]]"
  (rest (get-in z [4])))

(defn- churn-stats? [c]
  (= :change (get-in c [0])))

(defn- file-name [change]
  "I've only implemented line counts for git - make
   sure it works for hg too. A bit ugly for sure..."
  (if (churn-stats? change)
    (get-in change [3 1])
    (get-in change [1])))

(defn- files [z]
  (map file-name (changes z)))

(defn- make-row-constructor
  [v]
  (let [author (author v)
        rev (rev v)
        date (date v)]
    (fn [file]
      {:author author :rev rev :date date :entity file})))

(defn- entry-as-row
  "Transforms one entry (as a hiccup formated vector) into
   a map corresponding to a row in an Incanter dataset."
  [v]
  (let [row-ctor (make-row-constructor v)
        files (files v)]
    (map row-ctor files)))

(defn parse-log
  "Transforms the given input git log into an
   Incanter dataset suitable for the analysis modules." 
  [input grammar parse-options]
  (let [parser (insta/parser grammar)]
    (->>
     input
     (as-grammar-map parser)
     (mapcat entry-as-row)
     incanter/to-dataset)))
