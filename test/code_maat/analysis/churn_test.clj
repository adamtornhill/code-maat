;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.churn-test
  (:require [code-maat.analysis.churn :as churn]
            [code-maat.analysis.test-data :as test-data]
            [incanter.core :as incanter])
  (:use clojure.test))

(def ^:const incomplete
  [{:entity "A" :rev 1 :author "at" :date "2013-11-10"}
   {:entity "B" :rev 2 :author "ta" :date "2013-11-11"}])

(deftest throws-error-on-missing-modification-info
  "Some VCS (e.g. hg) don't provide the necessary metrics.
   In case a churn analysis is requested on such incomplete
   data we want to detect it early."
  (is (thrown? IllegalArgumentException
               (churn/absolutes-trend incomplete))))
