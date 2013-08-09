(ns code-maat.parsers.svn
  (:use [clojure.data.zip.xml :only (attr text xml-> xml1->)]) ; dep: see below
  (:require [code-maat.parsers.xml :as xml-parser]
            [clojure.xml :as xml]
            [clojure.zip :as zip]))

(defn zip->log-entries [zipped]
  (xml-> zipped :logentry))

(defn- make-extractor [logentry]
  (partial xml1-> logentry))

(defn- extract-modified-files [logentry]
  "Extracts all modified files from the given logentry."
  (let [paths (xml-> logentry :paths :path)
        files (filter #(= "file" (attr % :kind)) paths)
        modified-files (filter #(= "M" (attr % :action)) files)]
    (map text modified-files)))

(defn as-modification-set [svn-logentry]
  "Transforms the given svn logentry to a map containing
   the modification data."
  (let [extractor (make-extractor svn-logentry)]
    {:entities (extract-modified-files svn-logentry)
     :date (extractor :date text)
     :author (extractor :author text)
     :revision (extractor (attr :revision))}))

(defn zip->modification-sets [zipped]
  "Transforms the given zipped svn log into a seq
   of modification data."
  (map as-modification-set (zip->log-entries zipped)))