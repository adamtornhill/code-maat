;;; Copyright (C) 2015 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.time-parser
  (:require [clj-time.core :as tc]
            [clj-time.format :as tf]))

(def internal-time-format (tf/formatter "YYYY-MM-dd"))

(defn time-string-converter-from
  [format]
  (let [time-format (tf/formatter format)]
    (fn [time-as-string]
      (->>
       time-as-string
       (tf/parse time-format)
       (tf/unparse internal-time-format)))))

(defn time-parser-from
  [format]
  (let [time-format (tf/formatter format)]
    (fn [time-as-string]
      (tf/parse time-format time-as-string))))
