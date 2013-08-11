(ns code-maat.core
  (:require [code-maat.app.app :as app]))

(defn -main [logfile-name]
  (app/run logfile-name))
