(ns code-maat.analysis.test-data
  (:require [incanter.core :as incanter]))

(def ^:const vcs [{:author "apt" :entity "A" :rev 1}
          {:author "apt" :entity "B" :rev 1}
          {:author "apt" :entity "A" :rev 2}
          {:author "xy"  :entity "B" :rev 3}
          {:author "jt" :entity "A" :rev 4}])

(def ^:const vcsd (incanter/to-dataset vcs))