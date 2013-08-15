(ns code-maat.analysis.test-data
  (:require [incanter.core :as incanter]))

(def ^:const vcs [{:author "apt" :entity "A" :rev 1}
                  {:author "apt" :entity "B" :rev 1}
                  {:author "apt" :entity "A" :rev 2}
                  {:author "jt" :entity "A" :rev 3}])
(def ^:const vcsd (incanter/to-dataset vcs))

;;; Defines a dataset with a single entry to test one border case.
(def ^:const single-vcs [{:author "apt" :entity "A" :rev 1}])
(def ^:const single-vcsd (incanter/to-dataset single-vcs))

(def ^:const empty-vcsd (incanter/to-dataset []))

(defn content-of [ds]
  (:rows (incanter/sel
          ds
          :rows :all)))