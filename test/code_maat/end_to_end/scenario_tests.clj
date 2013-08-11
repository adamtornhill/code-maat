(ns code-maat.end-to-end.scenario-tests
  (:require [code-maat.parsers.svn :as svn]
            [code-maat.parsers.xml :as xml-parser]
            [clojure.data.zip.xml :as zip])
  (:use clojure.test))

(deftest generates-csv-summary-from-svn-log-file
  (= 1 1))
