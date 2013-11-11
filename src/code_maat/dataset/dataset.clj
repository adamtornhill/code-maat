;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.dataset.dataset
  (:require [incanter.core :as incanter]
            [code-maat.analysis.workarounds :as workarounds]))

;;; This module contains a thin layer around a subset of Incanter core.
;;; The reason is that Incanter is inconsistent in its return values:
;;;  - for multiple hits, a seq is returned,
;;;  - for a single hit, the sole value is returned.
;;; This behavior introduces a special case that we want to hide
;;; from our application level code.
;;;
;;; Thus, the responsibility of this module is to provide a uniform API by
;;; always returning a seq.

(defmacro def-ds
  "Defines a constant Incanter dataset based on
   the given vector data.
   This macro is typically used in the test cases."
  [name data]
  (let [ds-name (symbol (str name))]
    `(def ^:const ~ds-name
       (incanter/to-dataset ~data))))

(defn -empty?
  [ds]
  (= 0 (incanter/nrow ds)))

(defn -group-by
  [group-criterion ds]
  (if (-empty? ds)
    []
    (incanter/$group-by group-criterion ds)))

(defn -select-by
  [criterion ds]
  (workarounds/fix-single-return-value-bug
   (incanter/$ criterion ds)))

(defn -where
  [criterion ds]
  (incanter/$where criterion ds)) ; TODO: single value?

(defn -order-by
  [criterion order-fn ds]
  (incanter/$order criterion order-fn ds))

(defn -dataset
  [columns data]
  (incanter/dataset columns data))

(defn -nrows
  [ds]
  (incanter/nrow ds))
