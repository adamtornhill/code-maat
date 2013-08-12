(ns code-maat.analysis.workarounds)

(defn fix-single-return-value-bug
  "Workaround for what seems to be a flaw in Incanter.
   When returning a single value, that value is returned,
   not a seq."
  [r]
  (if (seq? r) r [r]))