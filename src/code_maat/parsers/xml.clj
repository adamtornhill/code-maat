;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.xml
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]))

(defn file->zip
  "Parses the given xml-file into a zipper data structure."
  [xml-file]
  (zip/xml-zip (xml/parse xml-file)))

(defn string->zip
  "Parses the given string into a zipper data structure."
  [s]
  (zip/xml-zip (xml/parse (new org.xml.sax.InputSource
                               (new java.io.StringReader s)))))
