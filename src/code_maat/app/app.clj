;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.app.app
  (:require [code-maat.parsers.svn :as svn]
            [code-maat.parsers.git :as git]
            [code-maat.parsers.mercurial :as hg]
            [code-maat.parsers.xml :as xml]
            [incanter.core :as incanter]
            [code-maat.output.csv :as csv-output]
            [code-maat.analysis.authors :as authors]
            [code-maat.analysis.entities :as entities]
            [code-maat.analysis.logical-coupling :as coupling]
            [code-maat.analysis.sum-of-coupling :as soc]
            [code-maat.analysis.summary :as summary]
            [code-maat.analysis.churn :as churn]
            [code-maat.analysis.effort :as effort]
            [code-maat.app.grouper :as grouper]
            [code-maat.analysis.communication :as communication]))

;;; Principles:
;;;
;;; All individual parts (parsers, analyses, outputs) are kept in
;;; separate, independet units.
;;;
;;; This top-level program (app - lousy name) glues the individual
;;; parts together into a pipeline of behaviour. The parts are
;;; selected based on the option passed in from the user interface.
;;;
;;; The overall flow is:
;;;  1 Input: raw text-files (log, optional layer spec, etc)
;;;  2 Parsers: receive the Input, returns a seq of maps. Each map
;;;    describes one modification.
;;;  3 The output from the parsers is fed into the layer mapping.
;;;    This is an optional step where individual changes may be
;;;    aggregated to fit analyses at architectural boundaries.
;;;  4 The seq of maps is now transformed into Incanter datasets.
;;;  5 The analyses receive the datasets. An analysis always returns
;;;    a dataset itself.
;;;  6 The output stage receives the dataset.


;;; TODO: consider making this dynamic in order to support new
;;;       analysis methods as plug-ins.
(def ^:const supported-analysis
  {"authors" authors/by-count
   "revisions" entities/by-revision
   "coupling" coupling/by-degree
   "soc" soc/by-degree
   "summary" summary/overview
   "identity" (fn [input _] input) ; for debugging - dumps all raw data
   "abs-churn" churn/absolutes-trend
   "author-churn" churn/by-author
   "entity-churn" churn/by-entity
   "entity-ownership" churn/as-ownership
   "entity-effort" effort/as-revisions-per-author
   "communication" communication/by-shared-entities})
   
(defn- make-analysis
  "Returns a seq of analysis methods closing over the options.
   Each analysis method takes a single data set as argument."
  [options]
  (if-let [analysis (supported-analysis (options :analysis))]
    [#(analysis % options)]
    (map (fn [a] #(a % options))
         (vals supported-analysis)))) ; :all

(defn- run-parser-in-error-handling-context
  [parse-fn vcs-name]
  (try
    (parse-fn)
    (catch IllegalArgumentException ae
      (throw ae))
    (catch Exception e
      (throw (IllegalArgumentException.
              (str vcs-name ": Failed to parse the given file - is it a valid logfile?"))))))

(defn- hg->modifications
  [logfile-name options]
  (run-parser-in-error-handling-context
   #(-> logfile-name slurp (hg/parse-log options))
   "Mercurial"))

(defn- svn-xml->modifications
  [logfile-name options]
  (run-parser-in-error-handling-context
   #(-> logfile-name xml/file->zip (svn/zip->modification-sets options))
   "svn"))

(defn- git->modifications
  [logfile-name options]
  (run-parser-in-error-handling-context
   #(-> logfile-name slurp (git/parse-log options))
   "git"))
  
(defn- parser-from
  [{:keys [version-control]}]
  (case version-control
    "svn" svn-xml->modifications
    "git" git->modifications
    "hg"  hg->modifications
    (throw (IllegalArgumentException.
            (str "Invalid --version-control specified: " version-control
                 ". Supported options are: svn or git.")))))

(defn- aggregate-on-boundaries
  "The individual changes may be aggregated into layers
   of architectural significance. This is done by re-mapping
   the name.
   In case there isn't any specified grouping, just work on
   the raw modifications."
  [options commits]
  (if-let [grouping (:group options)]
    (grouper/run grouping commits)
    commits))

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

(defn run
  "Runs the application using the given options.
   The options are a map with the following elements:
    :module - the VCS to parse
    :analysis - the type of analysis to run
    :rows - the max number of results to include"
  [logfile-name options]
  (let [vcs-parser (parser-from options)
        changes (vcs-parser logfile-name options)
        aggregated (aggregate-on-boundaries options changes)
        changes-ds (incanter/to-dataset aggregated)
        analysis (make-analysis options)
        output! (make-output options)]
    (doseq [an-analysis analysis]
      (run-with-recovery-point an-analysis changes-ds output!))))
  
