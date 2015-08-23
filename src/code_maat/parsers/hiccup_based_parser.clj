;;; Copyright (C) 2013-2015 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.hiccup-based-parser
  (:import [java.io BufferedReader StringReader])
  (:require [instaparse.core :as insta]
            [clojure.core.reducers :as r]
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

(defn- parse-entry
  [entry-token parse-fn]
  (parse-fn entry-token))

(defn- parse-entry-from
  [line-as-seq parse-fn]
  (->
   (s/join "\n" line-as-seq)
   (str "\n")
   (parse-entry parse-fn)))

(defn as-entry-tokens
  []
  (fn [rf]
    (let [acc (volatile! [])]
      (fn
        ([] (rf))
        ([result]
         (if-let [remaining (seq @acc)]
           (rf result remaining)
           (rf result)))
        ([result input]
         (if (s/blank? input)
           (let [remaining @acc]
             (vreset! acc [])
             (rf result remaining))
           (do
             (vswap! acc conj input)
             result)))))))

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
      (into [] (as-entry-tokens))
      (r/fold 32
              (fn
                ([] [])
                ([a b] (r/cat a b)))
              (fn [acc entry]
                (conj acc (parse-entry-from entry parse-fn))))
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
