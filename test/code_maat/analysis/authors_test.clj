(ns code-maat.analysis.authors-test
  (:require [code-maat.analysis.authors :as authors]
            [incanter.core :as incanter])
  (:use clojure.test))

(def vcs [{:author "apt" :entity "A" :rev 1}
          {:author "apt" :entity "B" :rev 1}
          {:author "apt" :entity "A" :rev 2}
          {:author "xy"  :entity "B" :rev 3}
          {:author "jt" :entity "A" :rev 4}])

(def vcsd (incanter/to-dataset vcs))

(deftest deduces-all-modified-entities ; wrong module for this - lower level! mining module?
  (= (authors/all-entities vcsd)
     #{"apt" "jt" "xy"}))

