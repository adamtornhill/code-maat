;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.math)

(defn average [x y]
  (/ (+ x y) 2))

(defn as-percentage [v]
  (* v 100))