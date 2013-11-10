;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.core
  (:gen-class)
  (:require [code-maat.app.app :as app]
            [clojure.tools.cli :as cli]))

(defn- as-app-options [args]
  (cli/cli args
           ["-vcs" "--version-control" "Input vcs module type: supports svn, git or hg"]
           ["-a" "--analysis" "The analysis to run (authors, revisions, coupling, summary, abs-churn, identity)"
            :default "authors"]
           ["-r" "--rows"   "Max rows in output" :default 10 :parse-fn #(Integer. %)]
           ["--min-revs" "Minimum number of revisions to include an entity in the analysis"
            :default 5 :parse-fn #(Integer. %)]
           ["--min-shared-revs" "Minimum number of shared revisions to include an entity in the analysis"
            :default 5 :parse-fn #(Integer. %)]
           ["--min-coupling" "Minimum degree of coupling (in percentage) to consider"
            :default 50 :parse-fn #(Integer. %)]
           ["--max-coupling" "Maximum degree of coupling (in percentage) to consider"
            :default 100 :parse-fn #(Integer. %)]))

(defn- print-banner []
  (let [[options args banner] (as-app-options [])]
    (println banner)))

(defn- input-file-from [args]
  (first args))

(defn -main
  ([]
     (print-banner))
  ([& args]
     (try
       (let [[options free-args banner]
             (as-app-options args)]
         (app/run (input-file-from free-args) options))
       (flush)
       (catch Exception e ; this is our main recovery point, errors propagate transparently to here
         (println "Error: " (.getMessage e))
         (print-banner)))))
