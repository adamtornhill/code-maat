(ns code-maat.parsers.svn
  (:use [clojure.data.zip.xml :only (attr text xml-> xml1->)]) ; dep: see below
  (:require [incanter.core :as incanter]
            [clj-time.core :as clj-time]
            [clj-time.format :as time-format]
            [clojure.xml :as xml]
            [clojure.zip :as zip]))

;;; This module contains functionality for parsing a generated SVN log
;;; into an Incanter dataset suitable for the supported analysis.
;;;
;;; Input: A SVN log on XML format.
;;; TODO: document the required SVN log content!

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
  (let [paths (xml-> logentry :paths :path)
        files (filter #(= "file" (attr % :kind)) paths)]
        (map group-file-with-action files)))

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

(def ^:const default-parse-options
  {:max-entries 200})

(defn- make-entries-limited-seq [parse-options s]
  (take (or (:max-entries parse-options) 200) s))

(def svn-date-formatter (time-format/formatters :date-time))

(defn- after-start-date?
  "A predicate that returns true if the given log-entry contains
   a time span after the start-time.
   The intent is to limit the commits included in the analysis.
   Over time, design issues get fixed and we don't want old
   data to interfere with our analysis results."
  [start-date log-entry]
  (let [extractor (make-extractor log-entry)
        entry-date (time-format/parse
                    svn-date-formatter
                    (extractor :date text))]
    (clj-time/after? entry-date start-date)))
    
(defn- make-date-span-limited-seq [parse-options s]
  (if-let [d (:date parse-options)]
    (take-while (partial after-start-date? d) s)
    s))

(defn- log-entries-to-include
  [parse-options s]
  (let [limitter (comp
                  (partial make-date-span-limited-seq parse-options)
                  make-entries-limited-seq)]
  (limitter parse-options s)))

(defn zip->modification-sets
  "Transforms the given zipped svn log into an Incanter
   dataset of modification data.
   The dataset contains the following rows:
   :entity :action :date :author :rev"
  ([zipped]
     (zip->modification-sets zipped default-parse-options))
  ([zipped parse-options]
     (->>
      zipped
      zip->log-entries
      (log-entries-to-include parse-options)
      ;(map as-rows)
      (reduce as-rows [])
      ;flatten
      incanter/to-dataset)))