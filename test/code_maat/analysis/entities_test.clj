(ns code-maat.analysis.entities-test
  (:require [code-maat.analysis.entities :as entities]
            [code-maat.analysis.test-data :as test-data])
  (:use clojure.test))

(deftest deduces-all-modified-entities
  (= (entities/all test-data/vcsd)
     #{"apt" "jt" "xy"}))