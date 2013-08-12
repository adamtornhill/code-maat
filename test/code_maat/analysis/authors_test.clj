(ns code-maat.analysis.authors-test
  (:require [code-maat.analysis.authors :as authors]
            [code-maat.analysis.test-data :as test-data]
            [incanter.core :as incanter])
  (:use clojure.test))

(deftest deduces-all-authors
  (is (= (authors/all test-data/vcsd)
         #{"apt" "jt"})))

(deftest gives-all-authors-of-specified-module
  (is (= (authors/of-module "A" test-data/vcsd)
         #{"apt" "jt"}))
  (is (= (authors/of-module "B" test-data/vcsd)
         #{"apt"})))

(defn- content-of [ds]
  (:rows (incanter/sel
          ds
          :rows :all)))

(deftest sorts-entities-on-max-number-of-authors
  (is (= (content-of (authors/by-count test-data/vcsd))
         [{:n-authors 2 :entity "A"}
          {:n-authors 1, :entity "B"}])))

(deftest sorts-order-is-optional
  (is (= (content-of (authors/by-count test-data/vcsd :asc))
         [{:n-authors 1 :entity "B"}
          {:n-authors 2, :entity "A"}])))
         
         
