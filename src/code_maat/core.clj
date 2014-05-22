;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.core
  (:gen-class)
  (:require [code-maat.app.app :as app]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]))

(def cli-options
  [["-l" "--log LOG" "Log file with input data"]
   ["-c" "--version-control VCS" "Input vcs module type: supports svn, git or hg"]
    ["-a" "--analysis ANALYSIS"
     "The analysis to run (authors, revisions, coupling, summary, churn, identity)"
     :default "authors"]
    ["-r" "--rows ROWS" "Max rows in output" :default 10 :parse-fn #(Integer/parseInt %)]
    ["-n" "--min-revs MIN-REVS" "Minimum number of revisions to include an entity in the analysis"
     :default 5 :parse-fn #(Integer/parseInt %)]
    ["-m" "--min-shared-revs MIN-SHARED-REVS" "Minimum number of shared revisions to include an entity in the analysis"
     :default 5 :parse-fn #(Integer/parseInt %)]
    ["-i" "--min-coupling MIN-COUPLING" "Minimum degree of coupling (in percentage) to consider"
     :default 50 :parse-fn #(Integer/parseInt %)]
    ["-x" "--max-coupling MAX-COUPLING" "Maximum degree of coupling (in percentage) to consider"
     :default 100 :parse-fn #(Integer/parseInt %)]
    ["-h" "--help"]])

(defn- usage [options-summary]
  (->> ["This is Code Maat, a program used to collect statistics from a VCS."
        ""
        "Usage: program-name log-file [options]"
        ""
        "Options:"
        options-summary
        "Please refer to the manual page for more information."]
       (string/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
     (:help options) (exit 0 (usage summary))
     errors (exit 1 (error-msg errors)))
    :else
    (try
      (app/run (:log options) options)
      (flush)
      (catch Exception e ; this is our main recovery point, errors propagate transparently to here
        (println "Error: " (.getMessage e))
        (exit 1 (usage summary))))))
