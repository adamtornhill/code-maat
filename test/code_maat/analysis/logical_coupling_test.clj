(ns code-maat.analysis.logical-coupling-test
  (:require [code-maat.analysis.logical-coupling :as coupling]
            [code-maat.analysis.test-data :as test-data]
            [incanter.core :as incanter])
  (:use clojure.test))

(def ^:const single-entity-commit
  [{:entity "This/is/a/single/entity" :rev 1}])

(def ^:const one-revision
  [{:entity "A" :rev 1}
   {:entity "B" :rev 1}
   {:entity "C" :rev 1}])

(def ^:const revd (incanter/to-dataset one-revision))

(def ^:const coupled
  [{:entity "A" :rev 1}
   {:entity "B" :rev 1}
   {:entity "C" :rev 1}
   {:entity "A" :rev 2}
   {:entity "B" :rev 2}])

(def ^:const coupledd (incanter/to-dataset coupled))

(def ^:const revd (incanter/to-dataset one-revision))

(deftest deduces-coupled-entities-in-the-same-revision
  (is (= (set (coupling/in-same-revision revd))
         (set [{:entity "A" :coupled "B"}
               {:entity "A" :coupled "C"}
               {:entity "B" :coupled "A"}
               {:entity "B" :coupled "C"}
               {:entity "C" :coupled "A"}
               {:entity "C" :coupled "B"}])))
  (testing "Workaround for Incanter's single value return instead of seq of one element."
    (is (= (coupling/in-same-revision (incanter/to-dataset single-entity-commit))
           []))))

(deftest calculates-commit-stats-for-each-couple
  (is (= (coupling/coupled-entities-with-rev-stats coupledd)
         [{:entity "A" :coupled "B" :shared-revs 2 :average-revs 2}
          {:entity "A" :coupled "C" :shared-revs 1 :average-revs 3/2}
          {:entity "B" :coupled "A" :shared-revs 2 :average-revs 2}
          {:entity "B" :coupled "C" :shared-revs 1 :average-revs 3/2}
          {:entity "C" :coupled "A" :shared-revs 1 :average-revs 3/2}
          {:entity "C" :coupled "B" :shared-revs 1 :average-revs 3/2}])))

(deftest calculates-coupling-by-its-degree
  (testing "With coupled entities"
    (is (= (incanter/to-list (coupling/by-degree coupledd))
           [["A" "B" 100]
            ["B" "A" 100]
            ["A" "C" 200/3]
            ["B" "C" 200/3]
            ["C" "A" 200/3]
            ["C" "B" 200/3]])))
  (testing "A single change set with a single entity (boundary case)"
    (is (= (incanter/to-list (coupling/by-degree
                              (incanter/to-dataset single-entity-commit)))
           []))))