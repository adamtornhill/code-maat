(ns code-maat.analysis.test-data
  (:require [incanter.core :as incanter]))

(def ^:const vcs [{:author "apt" :entity "A" :rev 1}
                  {:author "apt" :entity "B" :rev 1}
                  {:author "apt" :entity "A" :rev 2}
                  {:author "jt" :entity "A" :rev 3}])

(def ^:const vcsd (incanter/to-dataset vcs))

(defn content-of [ds]
  (:rows (incanter/sel
          ds
          :rows :all)))