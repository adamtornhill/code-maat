(ns code-maat.app.app
   (:require [code-maat.parsers.svn :as svn]
             [code-maat.parsers.xml :as xml]
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

(defn run [logfile-name output]
  (let [changes (xml->modifications logfile-name)
        most-authors (authors/by-count changes)
        most-revisions (entities/by-revision changes)]
    (output most-authors)
    (output most-revisions)))
  