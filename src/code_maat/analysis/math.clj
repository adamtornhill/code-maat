;;; Copyright (C) 2013-2014 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.math
  (:require [clojure.math.numeric-tower :as m]))

(defn average [& vals]
  (/
   (reduce + vals)
   (count vals)))

(defn as-percentage [v]
  (* v 100))

(defn ratio->centi-float-precision
  [v]
  (* 0.01 (m/round (* 100 (float v)))))
