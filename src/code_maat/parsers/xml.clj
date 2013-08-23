;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.xml
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]))

(defn file->zip [xml-file]
  "Parses the given xml-file into a zipper data structure."
  (zip/xml-zip (xml/parse xml-file)))

(defn string->zip [s]
  "Parses the given string into a zipper data structure."
  (zip/xml-zip (xml/parse (new org.xml.sax.InputSource
                               (new java.io.StringReader s)))))