(ns code-maat.core
  (:require [code-maat.app.app :as app]
            [clojure.tools.cli :as cli]))

(defn- as-app-options [args]
  (cli/cli args
           ["-m" "--module" "Input vcs module type: supports svn or git"]
           ["-o" "--output" "Output format: supports csv or graphs" :default "csv"]
           ["-a" "--analysis" "The analysis to run (authors, revisions, coupling)" :default :all]
           ["-r" "--rows"   "Max rows in output" :default 10 :parse-fn #(Integer. %)]
           ["-e" "--max-entries" "Max entries to parse in the input log file"
            :default 500 :parse-fn #(Integer. %)]))

(defn- print-banner []
  (let [[options args banner] (as-app-options [])]
    (println banner)))

;;;TODO: validate mandatory args, else throw!
(defn- input-file-from [args]
  (first args))

;;;TODO: introduce main catch block here - recovery point: print-banner
(defn -main
  ([]
     (print-banner))
  ([& args]
     (let [[options free-args banner]
           (as-app-options args)]
       (app/run (input-file-from free-args) options))))
