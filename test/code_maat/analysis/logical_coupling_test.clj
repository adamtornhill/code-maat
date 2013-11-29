;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

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

(deftest updates-entity-revision
  (testing "Creates stats the first time"
    (is (= (coupling/update-entity-rev-in {} "Entity")
           {"Entity" {:revs 1 :coupled {}}})))
  (testing "Increases existing revision count"
    (is (= (coupling/update-entity-rev-in {"Entity" {:revs 1 :coupled {}}}
                                          "Entity")
           {"Entity" {:revs 2 :coupled {}}})))
  (testing "Leaves other entities unaffected"
    (is (= (coupling/update-entity-rev-in {"Entity" {:revs 1 :coupled {}}
                                           "Other"  {:revs 3 :coupled {"C1" 1}}}
                                          "Entity")
           {"Entity" {:revs 2 :coupled {}}
            "Other"  {:revs 3 :coupled {"C1" 1}}}))))

(deftest updates-coupled-entities
  (let [stat-acc {"Entity" {:revs 2 :coupled {}}
                  "Other"  {:revs 3 :coupled {"C1" 1}}}]
    (is (= (coupling/update-coupling-in stat-acc {:entity "Entity" :coupled "C2"})
           {"Entity" {:revs 2 :coupled {"C2" 1}}
            "Other"  {:revs 3 :coupled {"C1" 1}}}))
     (is (= (coupling/update-coupling-in stat-acc {:entity "Other" :coupled "C2"})
           {"Entity" {:revs 2 :coupled {}}
            "Other"  {:revs 3 :coupled {"C1" 1 "C2" 1}}}))))

(def ^:const all-dependencies
  {"C"
   {:revs 1 :coupled {"B" 1, "A" 1}}
   "B"
   {:revs 2 :coupled {"C" 1, "A" 2}}
   "A"
   {:revs 2 :coupled {"C" 1, "B" 2}}})

(deftest calculates-change-dependencies
  (is (= (coupling/calc-dependencies coupledd)
        all-dependencies)))

(defn- no-threshold [& _] true)

(deftest calculates-logical-coupling-per-change
  (is (= (coupling/as-logical-coupling
          all-dependencies
          no-threshold
          ["A"
           {:revs 2 :coupled {"C" 1, "B" 2}}])
         [{:entity "A" :coupled "B" :degree 100 :average-revs 2}
          {:entity "A" :coupled "C" :degree 66 :average-revs 2}])))

(deftest calculates-coupling-by-its-degree
  (testing "With coupled entities"
    (is (= (incanter/to-list (coupling/by-degree
                              coupledd
                              test-data/options-with-low-thresholds))
           ;; :entity :coupled :degree :average-revs
           [["B"      "A"       100   2]
            ["A"      "B"       100   2]
            ["C"      "B"       66    2]
            ["C"      "A"       66    2]
            ["B"      "C"       66    2]
            ["A"      "C"       66    2]])))
  (testing "A single change set with a single entity (boundary case)"
    (is (= (incanter/to-list (coupling/by-degree
                              (incanter/to-dataset single-entity-commit)
                              test-data/options-with-low-thresholds))
           []))))
