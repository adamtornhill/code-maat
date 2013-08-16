(ns code-maat.app.app
   (:require [code-maat.parsers.svn :as svn]
             [code-maat.parsers.xml :as xml]
             [code-maat.output.csv :as csv-output]
             [code-maat.analysis.authors :as authors]
             [code-maat.analysis.entities :as entities]
             [code-maat.analysis.logical-coupling :as coupling]))
;;; TODO:
;;; - Parameterize the parse in order to shrink the paths (we move the files and
;;;   change the project structure a lot...).
;;; - Introduce a temporal period.

;;; TODO: consider making this dynamic in order to support new
;;;       analysis methods as plug-ins.
(def ^:const supported-analysis
  {"authors" authors/by-count
   "revisions" entities/by-revision
   "coupling" coupling/by-degree})

(defn- make-analysis
  [options]
  (if-let [analysis (supported-analysis (options :analysis))]
    [analysis]
    (vals supported-analysis))) ; :all

(defn- xml->modifications [logfile-name options]
  (svn/zip->modification-sets
   (xml/file->zip logfile-name)
   options))

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
  (let [changes (time (xml->modifications logfile-name options))
        analysis (make-analysis options)
        output (make-output options)]
    (doseq [an-analysis analysis]
      (output (time (an-analysis changes))))))
  