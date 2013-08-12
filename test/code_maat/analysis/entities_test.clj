(ns code-maat.analysis.entities-test
  (:require [code-maat.analysis.entities :as entities]
            [code-maat.analysis.test-data :as test-data])
  (:use clojure.test))

(deftest deduces-all-modified-entities
  (= (entities/all test-data/vcsd)
     #{"apt" "jt" "xy"}))

(deftest sorts-entities-on-number-of-revisions
  (is (= (test-data/content-of (entities/by-revision test-data/vcsd))
         [{:n-revs 3 :entity "A"}
          {:n-revs 1, :entity "B"}])))