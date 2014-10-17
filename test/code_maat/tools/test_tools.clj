;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.tools.test-tools
  (:require [code-maat.app.app :as app]
            [code-maat.test.data-driven :as dd]))

(defn run-with-str-output [log-file options]
  (with-out-str
    (app/run log-file options)))

(defmacro def-data-driven-with-vcs-test
  "Encapsulates the common pattern of iterating over a data driven
   test providing a vector of [file options] for each item.
   The body will execute with the symbols log-file and options bound to
   the different options in the test-data."
  [name test-data & body]
  `(dd/def-dd-test ~name
     [~'ddval# ~test-data]
     (let [[~'log-file ~'options] ~'ddval#]
       ~@body)))
