(ns code-maat.app.app
   (:require [code-maat.parsers.svn :as svn]
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

(defn- xml->modifications [logfile-name options]
  (try
    (svn/zip->modification-sets
     (xml/file->zip logfile-name)
     options)
    (catch Exception e
      (throw (IllegalArgumentException. "Failed to parse the given file - is it a valid svn logfile?")))))

;;; TODO: do not hardcode csv!
(defn- make-output [options]
  #(csv-output/write-to :stream % (:rows options)))

(defn run [logfile-name options]
  "Runs the application using the given options.
   The options are a map with the following elements:
    :module - the VCS to parse
    :output - the type of result output to generate
    :analysis - the type of analysis to run
    :rows - the max number of results to include"
  (let [changes (xml->modifications logfile-name options)
        analysis (make-analysis options)
        output (make-output options)]
    (doseq [an-analysis analysis]
      (output (an-analysis changes)))))
  