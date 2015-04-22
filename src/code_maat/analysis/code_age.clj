;;; Copyright (C) 2015 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.code-age
  (:require [code-maat.dataset.dataset :as ds]
            [code-maat.parsers.time-parser :as tp]
            [clj-time.core :as tc]
            [clojure.string :as str]
            [code-maat.analysis.math :as math]))

;;; The following analysis is inspired by Dan North's presentation
;;; on keeping a short software half-life.
;;;
;;; The Code Age analysis is based on the idea that we want
;;; to have code that's either:
;;;  1) So old that it's a commodity stored away in stable libraries, or
;;;  2) Fresh in our minds so that we remember what it does.
;;;
;;; The algorithms in this module will calculate the age of each
;;; entity in months with respect to the last time the code was
;;; modified. It's then up to us to visualize it in a sensible way.

(def time-parser (tp/time-parser-from "YYYY-MM-dd"))

(defn- as-time
  [time-as-string]
  (time-parser time-as-string))

(defn- time-now
  [options]
  (if-let [given-time (:age-time-now options)]
    (as-time given-time)
    (tc/now)))

(defn- changes-within-time-span
  [changes now]
  (ds/-where {:date {:$fn
                     (fn [date] (tc/before? (as-time date) now))}}
             changes))

(defn- latest-modification
  [changes]
  (->
   (ds/-select-by :date changes)
   sort
   last))

(defn- age-of-latest-in
  [changes now]
  (->
   (latest-modification changes)
   as-time
   (tc/interval now)
   tc/in-months))

(def has-content (complement ds/-empty?))

(defn- entities-by-latest-modification
  [now grouped]
  (for [[entity-entry changes] grouped
        :let [entity (:entity entity-entry)
              relevant-changes (changes-within-time-span changes now)]
        :when (has-content relevant-changes)]
    [entity (age-of-latest-in relevant-changes now)]))

(defn by-age
  [ds options]
  (->>
   (ds/-group-by :entity ds)
   (entities-by-latest-modification (time-now options))
   (ds/-dataset [:entity :age-months])
   (ds/-order-by [:age-months] :asc)))
  
