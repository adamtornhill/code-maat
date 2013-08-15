(ns code-maat.analysis.logical-coupling-test
  (:require [code-maat.analysis.logical-coupling :as coupling]
            [code-maat.analysis.test-data :as test-data]
            [incanter.core :as incanter])
  (:use clojure.test))

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
               {:entity "C" :coupled "B"}]))))

(deftest calculates-commit-stats-for-each-couple
  (is (= (coupling/coupled-entities-with-rev-stats coupledd)
         [{:entity "A" :coupled "B" :shared-revs 2 :average-revs 2}
          {:entity "A" :coupled "C" :shared-revs 2 :average-revs 3/2}
          {:entity "B" :coupled "A" :shared-revs 2 :average-revs 2}
          {:entity "B" :coupled "C" :shared-revs 2 :average-revs 3/2}
          {:entity "C" :coupled "A" :shared-revs 2 :average-revs 3/2}
          {:entity "C" :coupled "B" :shared-revs 2 :average-revs 3/2}])))

(deftest calculates-coupling-by-its-degree
  (is (= (incanter/to-list (coupling/by-degree coupledd))
         [["A" "C" 400/3]
          ["B" "C" 400/3]
          ["C" "A" 400/3] ; -> something's wrong!!
          ["C" "B" 400/3]
          ["A" "B" 100]
          ["B" "A" 100]])))