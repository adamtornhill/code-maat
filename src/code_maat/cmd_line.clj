;;; Copyright (C) 2013-2015 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.cmd-line
  (:gen-class)
  (:require [code-maat.app.app :as app]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]))

(def cli-options
  [["-l" "--log LOG" "Log file with input data"]
   ["-c" "--version-control VCS" "Input vcs module type: supports svn, git, git2, hg, p4, or tfs"]
   ["-a" "--analysis ANALYSIS"
    (str "The analysis to run (" (app/analysis-names)  ")")
    :default "authors"]
   [nil "--input-encoding INPUT-ENCODING" "Specify an encoding other than UTF-8 for the log file"]
   ["-r" "--rows ROWS" "Max rows in output" :parse-fn #(Integer/parseInt %)]
   ["-o" "--outfile OUTFILE" "Write the result to the given file name"]
   ["-g" "--group GROUP" "A file with a pre-defined set of layers. The data will be aggregated according to the group of layers."]
   ["-p" "--team-map-file TEAM-MAP-FILE" "A CSV file with author,team that translates individuals into teams."]
   ["-n" "--min-revs MIN-REVS" "Minimum number of revisions to include an entity in the analysis"
    :default 5 :parse-fn #(Integer/parseInt %)]
   ["-m" "--min-shared-revs MIN-SHARED-REVS" "Minimum number of shared revisions to include an entity in the analysis"
    :default 5 :parse-fn #(Integer/parseInt %)]
   ["-i" "--min-coupling MIN-COUPLING" "Minimum degree of coupling (in percentage) to consider"
    :default 30 :parse-fn #(Integer/parseInt %)]
   ["-x" "--max-coupling MAX-COUPLING" "Maximum degree of coupling (in percentage) to consider"
    :default 100 :parse-fn #(Integer/parseInt %)]
   ["-s" "--max-changeset-size MAX-CHANGESET-SIZE"
    "Maximum number of modules in a change set if it shall be included in a coupling analysis"
    :default 30 :parse-fn #(Integer/parseInt %)]
   ["-e" "--expression-to-match MATCH-EXPRESSION" "A regex to match against commit messages. Used with -messages analyses"]
   ["-t" "--temporal-period TEMPORAL-PERIOD"
    "Used for coupling analyses. Instructs Code Maat to consider all commits during the rolling temporal period as a single, logical commit set"]
   ["-d" "--age-time-now AGE-TIME_NOW" "Specify a date as YYYY-MM-dd that counts as time zero when doing a code age analysis"]
   [nil  "--verbose-results" "Includes additional analysis details together with the results. Only implemented for change coupling."]
   ["-h" "--help"]])

(defn- usage [options-summary]
  (->> ["This is Code Maat, a program used to collect statistics from a VCS."
        "Version: 1.0.5-SNAPSHOT"
        ""
        "Usage: program-name -l log-file [options]"
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
      (catch IllegalArgumentException e
        (println "Invalid argument: " (.getMessage e))
        (exit 1 (usage summary)))
      (catch Exception e ; this is our main recovery point
        (.printStackTrace e)
        (println "Error: " (.getMessage e))
        (exit 1 (usage summary))))))
