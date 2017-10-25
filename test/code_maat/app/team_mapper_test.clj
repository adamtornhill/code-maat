;;; Copyright (C) 2017 Adam Tornhill
;;;

(ns code-maat.app.team-mapper-test
  (:require [code-maat.app.team-mapper :as m])
  (:use [clojure.test]))

(def ^:private commits [{:entity "A" :rev 1 :author "X"}
                        {:entity "B" :rev 2 :author "Me Myself"}
                        {:entity "A" :rev 3 :author "X"}
                        {:entity "C" :rev 17 :author "Someone Else"}])

(deftest translates-authors-to-teams
  (testing "Maps all authors to the same team"
    (is (= [{:entity "A" :rev 1 :author "A Team"}
            {:entity "B" :rev 2 :author "A Team"}
            {:entity "A" :rev 3 :author "A Team"}
            {:entity "C" :rev 17 :author "A Team"}]
           (m/run commits {"X"            "A Team"
                           "Me Myself"    "A Team"
                           "Someone Else" "A Team"}))))
  (testing "Maps the authors to different teams"
    (is (= [{:entity "A" :rev 1 :author "C Team"}
            {:entity "B" :rev 2 :author "B Team"}
            {:entity "A" :rev 3 :author "C Team"}
            {:entity "C" :rev 17 :author "A Team"}]
           (m/run commits {"X"            "C Team"
                           "Me Myself"    "B Team"
                           "Someone Else" "A Team"}))))
  (testing "Unmapped authors are kept as-is"
    (is (= [{:entity "A" :rev 1 :author "X"} ; no mapping
            {:entity "B" :rev 2 :author "B Team"}
            {:entity "A" :rev 3 :author "X"}
            {:entity "C" :rev 17 :author "A Team"}] ; no mapping
           (m/run commits {"Me Myself"    "B Team"
                           "Someone Else" "A Team"})))))
