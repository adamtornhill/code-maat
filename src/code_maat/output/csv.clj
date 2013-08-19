(ns code-maat.output.csv
  (:require [code-maat.output.filters :as filters]
            [clojure.data.csv :as csv]
            [incanter.core :as incanter]))

;;; An output module presenting its given Incanter dataset on CSV format.

(defn write-to
  "Writes the given dataset ds as CSV to the given stream s.
   By default, all rows are written. This behavior
   is possible to override by providing a third argument
   specifying the number of rows to write."
  ([s ds]
     (csv/write-csv *out* [(map name (incanter/col-names ds))])
     (csv/write-csv *out* (incanter/to-list ds)))
  ([s ds n-rows]
     (write-to s (filters/n-rows ds n-rows))))