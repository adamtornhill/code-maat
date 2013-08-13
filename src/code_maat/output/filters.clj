(ns code-maat.output.filters
  (:require [incanter.core :as incanter]))

(defn n-rows [ds n]
  (let [safe-n (min n (incanter/nrow ds))]
    (incanter/sel ds :rows (range safe-n))))