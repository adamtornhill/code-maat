(ns code-maat.app.app
   (:require [code-maat.parsers.svn :as svn]
             [code-maat.parsers.xml :as xml]
             [code-maat.output.csv :as csv-output]
             [code-maat.analysis.authors :as authors]
             [code-maat.analysis.entities :as entities]))
;;; TODO:
;;; - Parameterize the parse in order to shrink the paths (we move the files and
;;;   change the project structure a lot...).
;;; - Introduce a temporal period.

(def ^:const vcs-parse-options
  {:max-entries 800})

(defn- xml->modifications [logfile-name]
  (svn/zip->modification-sets
   (xml/file->zip logfile-name)
   vcs-parse-options))

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
  (let [changes (xml->modifications logfile-name)
        most-authors (authors/by-count changes)
        most-revisions (entities/by-revision changes)
        output (make-output options)]
    (output most-authors)
    (output most-revisions)))
  