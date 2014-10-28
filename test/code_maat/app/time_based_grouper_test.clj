;;; Copyright (C) 2014 Adam Tornhill
;;;

(ns code-maat.app.time-based-grouper-test
  (:require [code-maat.app.app :as app])
  (:use [clojure.test]
        [code-maat.tools.test-tools]))

;;; End-to-end tests to simulate a time-based analysis.
;;;
;;; The test data contains two commits done the same day.
;;; With the default options we'll treat them as separate.
;;; In a time-base analysis we consider them as a logical
;;; part of the same work.

(def ^:const log-file "./test/code_maat/app/day_coupled_entities_git.txt")

(def ^:const csv-options
  {:version-control "git"
   :analysis "coupling"
   :min-revs 1
   :min-shared-revs 1
   :min-coupling 10
   :max-coupling 100
   :max-changeset-size 10})

(def ^:const csv-options-for-time-based
  (merge csv-options {:temporal-period "1"}))

(deftest only-calculates-coupling-within-same-commit-by-default
  (is (= (run-with-str-output log-file csv-options)
         "entity,coupled,degree,average-revs\n/Infrastrucure/Network/Connection.cs,/Presentation/Status/ClientPresenter.cs,100,1\n")))

(deftest calculates-coupling-within-same-day
  (is (= (run-with-str-output log-file csv-options-for-time-based)
         "entity,coupled,degree,average-revs\n/Infrastrucure/Network/Connection.cs,/Presentation/Status/ClientPresenter.cs,100,1\n/Infrastrucure/Network/Connection.cs,/Infrastrucure/Network/TcpConnection.cs,100,1\n/Infrastrucure/Network/TcpConnection.cs,/Presentation/Status/ClientPresenter.cs,100,1\n")))

(def ^:const options-with-invalid-time-period
  (merge csv-options {:temporal-period "2"}))

(deftest throws-on-unsupported-time-periods
  "We hope to support more options in the future."
  (is (thrown? IllegalArgumentException
               (run-with-str-output log-file options-with-invalid-time-period))))
