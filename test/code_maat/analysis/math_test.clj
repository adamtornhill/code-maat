;;; Copyright (C) 2016 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.math-test
  (:require [code-maat.analysis.math :as m])
  (:use clojure.test))

(deftest centi-float-precision
  "Test correct centi-float-precision."
  (is (= 1.0 (m/ratio->centi-float-precision 1.000)))
  (is (= 0.5 (m/ratio->centi-float-precision 0.5)))
  (is (= 0.67 (m/ratio->centi-float-precision 2/3)))
  (is (= 0.83 (m/ratio->centi-float-precision 5/6))))
