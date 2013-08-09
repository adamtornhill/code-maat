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
   <path
      kind='dir'
      action='M'>/Presentation/Status</path>
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
      action='M'>/Infrastrucure/Network/Connection.cs
    </path>
  </paths>
  <msg>[feature] Report connection status</msg>
 </logentry>
</log>
"))

(def log-entries (svn/zip->log-entries svn-log))
(def first-entry (first log-entries))

(deftest retrieves-all-entries-from-the-given-log
  (is (= (count log-entries)
         2)))

(deftest builds-modification-set
  (let [{:keys [author entities date revision]}
        (svn/as-modification-set first-entry)]
    (is (= author "APT"))
    (is (= (count entities) 2))
    (is (= date "2013-02-08T11:46:13.844538Z"))
    (is (= revision "2"))))

(deftest ignores-directory-entries
  (let [modifications (svn/as-modification-set first-entry)
        {:keys [entities]} modifications
        all-entities (zip/xml-> first-entry :paths :path)]
    (is (= (count entities) 2))
    (is (= (count all-entities) 3))))

(deftest builds-complete-modification-history-from-log
  "We know from the test above that details are OK => just
   check the quantities."
  (let [modifications (svn/zip->modification-sets svn-log)]
    (testing "parses all items"
      (is (= (count modifications)
             2)))
    (testing "parses the authors"
      (is (= (map :author modifications)
             ["APT" "XYZ"])))
    (testing "extracts all modifications"
      (is (= (count (flatten (map :entities modifications)))
             3)))))