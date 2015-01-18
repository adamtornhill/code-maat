;;; Copyright (C) 2013-2015 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.hiccup-based-parser
  (:import [java.io BufferedReader StringReader])
  (:require [instaparse.core :as insta]
            [clojure.string :as s]))

;;; This module encapsulates the common functionality of parsing a
;;; VCS log into Hiccup format using Instaparse.
;;; Clients parameterize this module with the actual grammar (e.g. git or hg).

(defn- raise-parse-failure
  [f input]
  (let [reason (with-out-str (print f))]
    (throw (IllegalArgumentException.
            (str "input: " input ", reason: " reason)))))

(defn parse-with
  "The actual invokation of the parser.
   Returns a Hiccup parse tree upon success,
   otherwise an informative exception is thrown."
  [parser input]
  (let [result (insta/parse parser input)]
    (if (insta/failure? result)
      (raise-parse-failure (insta/get-failure result) input)
      result)))
;;
;; Instaparse is great but consumes a lot of memory.
;; Thus, we cannot parse larger log files in a single pass.
;; Instead we need to tokenize the input stream into chunks
;; of entries that can be parsed one by one.
;;

(defn- entry-seq-as-string
  [e]
  (apply str e))

(defn as-entry-token
  [line-as-seq]
  (map #(str (first %) "\n") line-as-seq))

(defn- parse-entry
  [entry-token parse-fn]
  (->
   (entry-seq-as-string entry-token)
   parse-fn))

(defn- parse-entry-from
  [line-as-seq parse-fn]
  (->
   (as-entry-token line-as-seq)
   (parse-entry parse-fn)))

(defn- extend-when-complete
  "Keep each line wrapped in its own vector so that
   we're able to join them with a delimiter for the grammar."
  [entries next-line entry-acc parse-fn]
  (if (s/blank? next-line)
    [(conj entries (parse-entry-from entry-acc parse-fn)) []]
    [entries (conj entry-acc [next-line])]))

(defn- complete-the-rest-in
  "Constructs the final entry in the version-control log.
   The entries are separated by a blank line _except_ for
   the last one that doesn't get a trailing newline."
  [entry-acc entries parse-fn]
  (if (empty? entry-acc)
    entries
    (conj entries (parse-entry-from entry-acc parse-fn))))

(defn as-entry-tokens
  [parse-fn lines]
  (loop [lines-left lines
         entry-acc []
         entries []]
    (if (empty? lines-left)
      (complete-the-rest-in entry-acc entries parse-fn)
      (let [next-line (first lines-left)
            [updated-entries updated-acc]  (extend-when-complete entries
                                                                 next-line
                                                                 entry-acc
                                                                 parse-fn)]
        (recur (rest lines-left) updated-acc updated-entries)))))
  
;;
;; Transform the Instaparse Hiccup vectors to our own representation (maps)
;;

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

;;; TODO: clean-up: most of the pipeline can be built
;;; as a combination of the different functions (including
;;; the second pass through as we transform from hiccup).

(defn- parse-from
  "Expected to be invoked in a with-open context."
  [rdr grammar field-extractors]
   (let [specific-parser (insta/parser grammar)
         parse-fn (partial parse-with specific-parser)]
     (->>
      (line-seq rdr)
      (as-entry-tokens parse-fn)
      (mapcat (partial entry-as-row field-extractors)))))

(defn- encoding-from
  [options]
  (get options :input-encoding "UTF-8"))

(defn parse-log
  "Transforms the given input git log into a
   seq of maps suitable for the analysis modules."
  [input-file-name options grammar field-extractors]
  (with-open [rdr (clojure.java.io/reader
                   input-file-name
                   :encoding (encoding-from options))]
    (parse-from rdr grammar field-extractors)))

(defn parse-read-log
  [log-text options grammar field-extractors]
  (with-open [rdr (BufferedReader. (StringReader. log-text))]
    (parse-from rdr grammar field-extractors)))
