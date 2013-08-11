(ns code-maat.app.app
   (:require [code-maat.parsers.svn :as svn]
            [code-maat.parsers.xml :as xml]
            [code-maat.analysis.authors :as authors]))
;;; TODO:
;;; - Parameterize the parse in order to shrink the paths (we move the files and
;;;   change the project structure a lot...).
;;; - Introduce a temporal period.

(defn- xml->modifications [logfile-name]
  (-> logfile-name
      xml/file->zip
      svn/zip->modification-sets))

(defn- present-author-stats [a]
  (println "Author statistis:")
  (println a))

(defn run [logfile-name]
  (let [changes (xml->modifications logfile-name)
        most-authors (authors/by-count changes)]
    (present-author-stats most-authors)))
  