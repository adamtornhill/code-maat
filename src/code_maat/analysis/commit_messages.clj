;;; Copyright (C) 2014 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.commit-messages
  (:require [code-maat.dataset.dataset :as dataset]
            [incanter.core :as incanter]
            [code-maat.analysis.math :as math]))

;;; This module helps you analyze version-control data
;;; based on commit messages.
;;; Our commit messages contain information about our process and
;;; the kind of work we do.
;;; For example, we can use that information to extract
;;; statistics on bug distributions. Note that this data is 
;;; heuristics, not absolute truths (for that you need to mine
;;; your bug tracking system).
;;;
;;; Usage: just provide a regular expression that specifies
;;; the words of interest in :expression-to-match.


(defn- as-word-match-expr
  [raw-expr]
  (re-pattern raw-expr))

(defn- match-expr-from
  [options]
  (if-let [mexpr (:expression-to-match options)]
    (as-word-match-expr mexpr)
    (throw
     (IllegalArgumentException.
      "Commit messages: you need to provide an expression to match against."))))

(defn- columns-of-interest
  "Filter away all the data we won't need (less GC overhead)."
  [ds]
  (dataset/-select-by [:entity :message] ds))

(defn- commit-matches
  "Performs a match for the expression provided by the
   user against a single commit message."
  [mexpr line]
  (re-find mexpr line))

(defn- rows-matching-given-expr
  [mexpr ds]
  (dataset/-where {:message  {:$fn (fn [m] (commit-matches mexpr m))}} ds))

(defn- as-matching-entity-freqs
  [ds]
  (->>
   (dataset/-select-by :entity ds)
   incanter/to-vect
   frequencies
   (into [])))

(defn by-word-frequency
  "Returns the frequencies of the given word matches
   across all entities.
   This analysis is typically used to extrapolate
   bug fixes from the commit messages.
   For example, the user may specify a list of
   suspicious words like bug, error, etc and
   this function counts the occourences."
  [ds options]
  (->>
   (rows-matching-given-expr (match-expr-from options) ds)
   as-matching-entity-freqs
   (incanter/dataset [:entity :matches])
   (dataset/-order-by [:matches :entity] :desc)))
