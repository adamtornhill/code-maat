(ns code-maat.output.filters
  (:require [incanter.core :as incanter]))

(defn n-rows [ds n]
  (incanter/sel ds :rows (range n)))