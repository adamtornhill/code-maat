;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.svn
  (:use [clojure.data.zip.xml :only (attr text xml-> xml1->)]) ; dep: see below
  (:require [incanter.core :as incanter]
            [code-maat.parsers.limitters :as limiter]
            [clj-time.core :as clj-time]
            [clj-time.format :as time-format]
            [clojure.xml :as xml]
            [clojure.zip :as zip]))

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
  (let [entity-name (text entry)
        svn-action (attr entry :action)
        action (get svn-action->interpretable-action svn-action)]
    [entity-name action]))

(defn- extract-modified-files [logentry]
  "Extracts all modified files from the given logentry."
  (let [paths (xml-> logentry :paths :path)]
    (map group-file-with-action paths)))

(defn as-rows [coll svn-logentry]
  "Transforms the given svn logentry to a seq of rows containing
   the modification data for each entity."
  (let [extractor (make-extractor svn-logentry)
        entities (extract-modified-files svn-logentry)
        date (extractor :date text)
        author (extractor :author text)
        revision (extractor (attr :revision))]
    (reduce conj
            coll
            (for [[e action] entities
                  :let [row {:entity e :action action :date date :author author :rev revision}]]
              row))))

(def svn-date-formatter (time-format/formatters :date-time))

(defn- make-date-filter
  [filter-fn log-entry]
  (let [extractor (make-extractor log-entry)
        entry-date (time-format/parse
                    svn-date-formatter
                    (extractor :date text))]
    (filter-fn entry-date)))

(defn- date-of
  [log-entry]
  (let [extractor (make-extractor log-entry)]
    (time-format/parse
     svn-date-formatter
     (extractor :date text))))

(defn- parse
  [zipped parse-options]
  (->>
   zipped
   zip->log-entries
   (limiter/log-entries-to-include parse-options date-of)
   (reduce as-rows [])
   incanter/to-dataset))

(defn zip->modification-sets
  "Transforms the given zipped svn log into an Incanter
   dataset of modification data.
   The dataset contains the following rows:
   :entity :action :date :author :rev"
  ([zipped]
     (zip->modification-sets zipped {}))
  ([zipped parse-options]
     (parse zipped parse-options)))