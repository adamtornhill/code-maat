;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.output.filters
  (:require [incanter.core :as incanter]))

(defn n-rows [ds n]
  (let [safe-n (min n (incanter/nrow ds))]
    (incanter/sel ds :rows (range safe-n))))