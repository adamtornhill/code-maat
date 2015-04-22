;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.svn
  (:use [clojure.data.zip.xml :only (attr text xml-> xml1->)]) ; dep: see below
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [code-maat.parsers.time-parser :as tp]
            [clojure.string :as s]))

;;; This module contains functionality for parsing a generated SVN log
;;; into a map suitable for the supported analysis.
;;;
;;; Input: A SVN log on XML format.
;;;
;;; Ouput: A seq of maps with the following keys:
;;;   :entity :action :date :author :rev

(defn zip->log-entries [zipped]
  (xml-> zipped :logentry))

(defn- make-extractor [logentry]
  (partial xml1-> logentry))

(defn- group-file-with-action [entry]
  (let [entity-name (s/trimr (text entry))
        svn-action (attr entry :action)]
    [entity-name svn-action]))

(defn- extract-modified-files
  "Extracts all modified files from the given logentry."
  [logentry]
  (let [paths (xml-> logentry :paths :path)]
    (map group-file-with-action paths)))

(def as-common-time-format (tp/time-string-converter-from "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"))

(defn as-rows
  "Transforms the given svn logentry to a seq of rows containing
   the modification data for each entity."
  [svn-logentry]
  (let [extractor (make-extractor svn-logentry)
        entities (extract-modified-files svn-logentry)
        date (extractor :date text)
        author (extractor :author text)
        revision (extractor (attr :revision))]
    (map (fn [[entity action]]
           {:entity entity
            :date (as-common-time-format date)
            :author author
            :action action
            :rev revision})
         entities)))

(defn- parse
  [zipped parse-options]
  (->>
   zipped
   zip->log-entries
   (mapcat as-rows)))

(defn zip->modification-sets
  "Transforms the given zipped svn log into a map
   of modification data.
   The map contains the following rows:
   :entity :action :date :author :rev"
  ([zipped]
     (zip->modification-sets zipped {}))
  ([zipped parse-options]
     (parse zipped parse-options)))
