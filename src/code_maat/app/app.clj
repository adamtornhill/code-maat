;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.app.app
  (:require [code-maat.parsers.svn :as svn]
            [code-maat.parsers.git :as git]
            [code-maat.parsers.xml :as xml]
            [code-maat.output.csv :as csv-output]
            [code-maat.analysis.authors :as authors]
            [code-maat.analysis.entities :as entities]
            [code-maat.analysis.logical-coupling :as coupling]))

;;; TODO: consider making this dynamic in order to support new
;;;       analysis methods as plug-ins.
(def ^:const supported-analysis
  {"authors" authors/by-count
   "revisions" entities/by-revision
   "coupling" coupling/by-degree1})

(defn- make-analysis
  "Returns a seq of analysis methods closing over the options.
   Each analysis method takes a single data set as argument."
  [options]
  (if-let [analysis (supported-analysis (options :analysis))]
    [#(analysis % options)]
    (map (fn [a] #(a % options))
         (vals supported-analysis)))) ; :all

(defn- svn-xml->modifications
  [logfile-name options]
  (try
    (svn/zip->modification-sets
     (xml/file->zip logfile-name)
     options)
    (catch Exception e
      (throw (IllegalArgumentException.
              "Failed to parse the given file - is it a valid svn logfile?")))))

(defn- git->modifications
  [logfile-name options]
  (try
    (->
     logfile-name
     slurp
     git/parse-log)
    (catch IllegalArgumentException ae
      (throw ae))
    (catch Exception e
      (throw (IllegalArgumentException.
              "Failed to parse the given file - is it a valid git logfile?")))))
  
(defn- get-parser
  [{:keys [version-control]}]
  (case version-control
    "svn" svn-xml->modifications
    "git" git->modifications
    (throw (IllegalArgumentException.
            (str "Invalid --version-control specified: " version-control
                 ". Supported options are: svn or git.")))))

(defn- make-output [options]
  (if-let [n-out-rows (:rows options)]
    #(csv-output/write-to :stream % n-out-rows)
    #(csv-output/write-to :stream %)))

(defn- throw-internal-error [e]
  (throw (IllegalArgumentException.
          (str "Internal error - please report it. Details = "
               (.getMessage e)))))

(defn- run-with-recovery-point
  [analysis-fn changes output-fn!]
  (try
    (output-fn! (analysis-fn changes))
    (catch AssertionError e ; typically a pre- or post-condition
      (throw-internal-error e))
    (catch Exception e
      (throw-internal-error e))))

(defn run [logfile-name options]
  "Runs the application using the given options.
   The options are a map with the following elements:
    :module - the VCS to parse
    :output - the type of result output to generate
    :analysis - the type of analysis to run
    :rows - the max number of results to include"
  (let [vcs-parser (get-parser options)
        changes (vcs-parser logfile-name options)
        analysis (make-analysis options)
        output! (make-output options)]
    (doseq [an-analysis analysis]
      (run-with-recovery-point an-analysis changes output!))))
  