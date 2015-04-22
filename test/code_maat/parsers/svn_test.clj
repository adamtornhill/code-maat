;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.svn-test
  (:require [code-maat.parsers.svn :as svn]
            [code-maat.parsers.xml :as xml-parser]
            [clojure.data.zip.xml :as zip])
  (:use clojure.test))

;;; A sample from a svn log file, served as test data to the unit tests.
(def svn-log (xml-parser/string->zip "<?xml version='1.0'?>
<log>
 <logentry
   revision='2'>
  <author>APT</author>
  <date>2013-02-08T11:46:13.844538Z</date>
  <paths>
    <path
      kind='file'
      action='M'>/Infrastrucure/Network/Connection.cs
    </path>
   <path
     kind='file'
     action='M'>/Presentation/Status/ClientPresenter.cs
   </path>
  </paths>
  <msg>[bug] Fixed the connection status </msg>
 </logentry>
 <logentry
   revision='1'>
  <author>XYZ</author>
  <date>2013-02-07T11:46:13.844538Z</date>
  <paths>
    <path
      kind='file'
      action='A'>/Infrastrucure/Network/Connection.cs
    </path>
  </paths>
  <msg>[feature] Report connection status</msg>
 </logentry>
</log>
"))

(def log-entries (svn/zip->log-entries svn-log))
(def first-entry (first log-entries))
(def second-entry (second log-entries))

(deftest retrieves-all-entries-from-the-given-log
  (is (= (count log-entries)
         2)))

(deftest one-modified-entity-per-row
  (let [[row1 row2] (svn/as-rows first-entry)]
    (is (= row1
           {:entity "/Infrastrucure/Network/Connection.cs"
            :date "2013-02-08"
            :author "APT"
            :action "M":rev "2"}))
    (is (= row2
           {:entity "/Presentation/Status/ClientPresenter.cs"
            :date "2013-02-08"
            :author "APT"
            :action "M"
            :rev "2"}))))

(deftest created-entities-are-marked
  (let [[row-with-created-entity] (svn/as-rows second-entry)]
    (is (= row-with-created-entity
           {:entity "/Infrastrucure/Network/Connection.cs"
            :date "2013-02-07"
            :author "XYZ"
            :action "A"
            :rev "1"}))))

(deftest builds-complete-modification-history-from-log
  "We know from the test above that details are OK => just
   check the quantities."
  (let [modifications (svn/zip->modification-sets svn-log)]
    (testing "parses all items"
      (is (= (count modifications)
             3)))
    (testing "parses the log info into each row"
      (is (= (map :author modifications)
             ["APT" "APT" "XYZ"]))
      (is (= (map :entity modifications)
             ["/Infrastrucure/Network/Connection.cs"
              "/Presentation/Status/ClientPresenter.cs"
              "/Infrastrucure/Network/Connection.cs"])))))
