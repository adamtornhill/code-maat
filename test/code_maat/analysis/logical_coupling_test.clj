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

;;; New

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

(deftest calculates-logical-coupling-per-change
  (is (= (coupling/as-logical-coupling
          all-dependencies
          ["A"
           {:revs 2 :coupled {"C" 1, "B" 2}}])
         [{:entity "A" :coupled "B" :degree 100}
          {:entity "A" :coupled "C" :degree 200/3}])))

(deftest calculates-coupling-by-its-degree
  (testing "With coupled entities"
    (is (= (incanter/to-list (coupling/by-degree1 coupledd))
           [["B" "A" 100]
            ["A" "B" 100]
            ["C" "B" 200/3]
            ["C" "A" 200/3]
            ["B" "C" 200/3]
            ["A" "C" 200/3]])))
  (testing "A single change set with a single entity (boundary case)"
    (is (= (incanter/to-list (coupling/by-degree1
                              (incanter/to-dataset single-entity-commit)))
           []))))

;;; End new

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

(deftest calculates-coupling-by-its-degree1
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