(ns code-maat.parsers.svn
  (:use [clojure.data.zip.xml :only (attr text xml-> xml1->)]) ; dep: see below
  (:require [incanter.core :as incanter]
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

(defn as-rows [svn-logentry]
  "Transforms the given svn logentry to a seq of rows containing
   the modification data for each entity."
  (let [extractor (make-extractor svn-logentry)
        entities (extract-modified-files svn-logentry)
        date (extractor :date text)
        author (extractor :author text)
        revision (extractor (attr :revision))]
    (for [[e action] entities
          :let [row {:entity e :action action :date date :author author :rev revision}]]
      row)))

(def ^:const default-parse-options
  {:max-entries 200})

(defn zip->modification-sets
  "Transforms the given zipped svn log into an Incanter
   dataset of modification data.
   The dataset contains the following rows:
   :entity :action :date :author :rev"
  ([zipped]
     (zip->modification-sets zipped default-parse-options))
  ([zipped parse-options]
     (incanter/to-dataset
      (flatten
       (map as-rows
            (take (:max-entries parse-options)
                  (zip->log-entries zipped)))))))