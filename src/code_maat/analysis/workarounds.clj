;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.workarounds)

(defn fix-single-return-value-bug
  "Workaround for what seems to be a flaw in Incanter.
   When returning a single value, that value is returned,
   not a seq."
  [r]
  (if (seq? r) r [r]))