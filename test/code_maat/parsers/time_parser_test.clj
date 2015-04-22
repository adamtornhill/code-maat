;;; Copyright (C) 2015 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.time-parser-test
  (:require [code-maat.parsers.time-parser :as p])
  (:use clojure.test))

(deftest parses-git-format
  (let [parser (p/time-string-converter-from "YYYY-MM-dd")
        date-to-parse "2014-12-26"]
    (is (= (parser date-to-parse)
           date-to-parse))))
