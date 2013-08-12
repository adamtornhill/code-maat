(ns code-maat.core
  (:require [code-maat.app.app :as app]
            [code-maat.output.csv :as csv-output]))

(defn- make-csv-output [n-rows-in-output]
  #(csv-output/write-to :stream % n-rows-in-output))

(defn -main [logfile-name]
  (app/run
   logfile-name
   (make-csv-output 10)))
