(ns code-maat.analysis.authors-test
  (:require [code-maat.analysis.authors :as authors]
            [code-maat.analysis.test-data :as test-data])
  (:use clojure.test))

(deftest deduces-all-authors
  (= (authors/all test-data/vcsd)
     #{"apt" "jt" "xy"}))

