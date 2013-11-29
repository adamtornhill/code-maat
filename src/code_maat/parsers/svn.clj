;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.svn
  (:use [clojure.data.zip.xml :only (attr text xml-> xml1->)]) ; dep: see below
  (:require [incanter.core :as incanter]
            [clojure.xml :as xml]
            [code-maat.dataset.dataset :as ds]
            [clojure.zip :as zip]
            [clojure.string :as s]))

;;; This module contains functionality for parsing a generated SVN log
;;; into an Incanter dataset suitable for the supported analysis.
;;;
;;; Input: A SVN log on XML format.
;;;
;;; Ouput: An incanter dataset with the following columns:
;;;   :entity :action :date :author :rev

(defn zip->log-entries [zipped]
  (xml-> zipped :logentry))

(defn- make-extractor [logentry]
  (partial xml1-> logentry))

(def ^:const svn-action->interpretable-action
  {"A" :created
   "M" :modified
   "D" :deleted
   "R" :moved})

(defn- group-file-with-action [entry]
  (let [entity-name (s/trimr (text entry))
        svn-action (attr entry :action)
        action (get svn-action->interpretable-action svn-action)]
    [entity-name action]))

(defn- extract-modified-files
  "Extracts all modified files from the given logentry."
  [logentry]
  (let [paths (xml-> logentry :paths :path)]
    (map group-file-with-action paths)))

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
           [entity date author action revision])
         entities)))

(defn- parse
  [zipped parse-options]
  (->>
   zipped
   zip->log-entries
   (mapcat as-rows)
   (ds/-dataset [:entity :date :author :action :rev])))

(defn zip->modification-sets
  "Transforms the given zipped svn log into an Incanter
   dataset of modification data.
   The dataset contains the following rows:
   :entity :action :date :author :rev"
  ([zipped]
     (zip->modification-sets zipped {}))
  ([zipped parse-options]
     (parse zipped parse-options)))
