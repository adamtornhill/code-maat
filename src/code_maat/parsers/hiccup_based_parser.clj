;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.hiccup-based-parser
  (:require [instaparse.core :as insta]))

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

(defn- churn-stats? [c]
  (= :change (get-in c [0])))

(defn- file-stats [change]
  "The file statistics are at least the name of the file.
   Some VCS (git) allows me to easily include churn stats.
   If they're available, I parse them too.
   Example:
    [[:change [:added 10] [:deleted 9] [:file src/code_maat/parsers/git.clj]]"
  (if (churn-stats? change)
    {:name (get-in change [3 1])
     :added (get-in change [1 1])
     :deleted (get-in change [2 1])}
    {:name (get-in change [1])}))

(defn- files [{:keys [changes]} z]
  (map file-stats (changes z)))

(defn- make-row-constructor
  [{:keys [author rev date message]} v]
  (let [author-value (author v)
        rev-value (rev v)
        date-value (date v)
        message-value (message v)]
    (fn [{:keys [name added deleted]}]
      (let [mandatory  {:author author-value
                        :rev rev-value
                        :date date-value
                        :entity name
                        :message message-value}
            optional {:loc-added added
                      :loc-deleted deleted}]
        (if (and added deleted)
          (merge mandatory optional)
          mandatory)))))

(defn- entry-as-row
  "Transforms one entry (as a hiccup formated vector) into
   a map representing one row in the change information."
  [field-extractors v]
  (let [row-ctor (make-row-constructor field-extractors v)
        files (files field-extractors v)]
    (map row-ctor files)))

(defn parse-log
  "Transforms the given input git log into a
   seq of maps suitable for the analysis modules." 
  [input grammar parse-options field-extractors]
  (let [parser (insta/parser grammar)]
    (->>
     input
     (as-grammar-map parser)
     (mapcat (partial entry-as-row field-extractors)))))
