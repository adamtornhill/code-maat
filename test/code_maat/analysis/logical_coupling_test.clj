(ns code-maat.analysis.logical-coupling-test
  (:require [code-maat.analysis.logical-coupling :as coupling]
            [code-maat.analysis.test-data :as test-data]
            [incanter.core :as incanter])
  (:use clojure.test))

(def ^:const one-revision
  [{:author "apt" :entity "A" :rev 1}
   {:author "apt" :entity "B" :rev 1}
   {:author "apt" :entity "C" :rev 1}])

(def ^:const revd (incanter/to-dataset one-revision))

(deftest deduces-coupled-entities-in-the-same-revision
  (is (= (set (coupling/in-same-revision revd))
         (set [{:entity "A" :coupled "B"}
               {:entity "A" :coupled "C"}
               {:entity "B" :coupled "A"}
               {:entity "B" :coupled "C"}
               {:entity "C" :coupled "A"}
               {:entity "C" :coupled "B"}]))))