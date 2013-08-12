(ns code-maat.app.app
   (:require [code-maat.parsers.svn :as svn]
             [code-maat.parsers.xml :as xml]
             [incanter.core :as incanter]
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

(defn- pick-top-offenders [ds]
  (incanter/sel ds :rows (range 10)))

(defn- present-author-stats [a]
  (println "Author statistis:")
  (println (pick-top-offenders a)))

(defn- present-revision-stats [r]
  (println "Entities by revision:")
  (println (pick-top-offenders r)))

(defn run [logfile-name]
  (let [changes (xml->modifications logfile-name)
        most-authors (authors/by-count changes)
        most-revisions (entities/by-revision changes)]
    (present-author-stats most-authors)
    (present-revision-stats most-revisions)))
  