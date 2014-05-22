;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.communication-test
  (:require [code-maat.analysis.communication :as communication]
            [code-maat.dataset.dataset :as ds])
  (:use clojure.test))

(def ^:const options {})

(ds/def-ds sharing-authors
  [{:entity "A" :rev 1 :author "at" :date "2013-11-10"}
   {:entity "A" :rev 2 :author "jt" :date "2013-11-11"}
   {:entity "A" :rev 3 :author "ap" :date "2013-11-15"}
   {:entity "B" :rev 4 :author "at" :date "2013-11-23"}
   {:entity "B" :rev 5 :author "jt" :date "2013-11-23"}])

(deftest calculates-communication-needs-for-shared-authorship
  (is (= (communication/by-shared-entities sharing-authors options)
         (ds/-dataset [:author :peer :shared :average :strength]
                      [["jt" "at" 2 2 100 ]
                       ["at" "jt" 2 2 100 ]
                       ["jt" "ap" 1 2  50]
                       ["at" "ap" 1 2  50]
                       ["ap" "jt" 1 2  50]
                       ["ap" "at" 1 2  50]]))))
