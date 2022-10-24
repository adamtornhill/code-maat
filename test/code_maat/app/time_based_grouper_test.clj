(ns code-maat.app.time-based-grouper-test
  (:require [code-maat.app.time-based-grouper :as grouper])
  (:use [clojure.test]))

(deftest commits-by-day
  (testing "Expect a non-modifying operation"
    (let [input-commits [{:entity "A" :rev 1 :date "2022-10-20"}
                         {:entity "B" :rev 2 :date "2022-10-20"}]]
      (is (= [{:date   "2022-10-20"  :entity "A" :rev    "2022-10-20"}
               {:date   "2022-10-20" :entity "B" :rev    "2022-10-20"}]
             (grouper/by-time-period input-commits {:temporal-period "1"}))))))

(deftest multiple-days-give-a-rolling-dataset
  (let [input-commits [{:entity "A" :rev 1 :date "2022-10-20"}
                       {:entity "B" :rev 2 :date "2022-10-20"}

                       {:entity "B" :rev 3 :date "2022-10-19"} ; double entry, two B's when looking at last two days
                       {:entity "D" :rev 3 :date "2022-10-19"}

                       {:entity "C" :rev 4 :date "2022-10-18"}
                       {:entity "D" :rev 4 :date "2022-10-18"}

                       {:entity "D" :rev 5 :date "2022-10-15"}]] ; a gap in days between the commits
    (is (= [
            ; Only commits on 2022-10-15, not on subsequent day:
            {:date   "2022-10-15" :entity "D" :rev    "2022-10-15"}

            ; 17-18th
            {:date   "2022-10-18" :entity "C" :rev    "2022-10-18"}
            {:date   "2022-10-18" :entity "D" :rev    "2022-10-18"}

            ; 18-19th
            {:date   "2022-10-18" :entity "C" :rev    "2022-10-19"}
            {:date   "2022-10-18" :entity "D" :rev    "2022-10-19"}
            {:date   "2022-10-19" :entity "B" :rev    "2022-10-19"}

            ; 19-20th
            {:date   "2022-10-19" :entity "B" :rev    "2022-10-20"}
            {:date   "2022-10-19" :entity "D" :rev    "2022-10-20"}
            {:date   "2022-10-20" :entity "A" :rev    "2022-10-20"}]
           (grouper/by-time-period input-commits {:temporal-period "2"})))))

(deftest edge-cases
  (testing "Works on an empty input sequence, ie. no commits"
    (is (= []
           (grouper/by-time-period [] {:temporal-period "2"}))))
  (testing "Works on a single commit"
    (is (= [{:date   "2022-10-19" :entity "B" :rev    "2022-10-19"}]
           (grouper/by-time-period [{:entity "B" :rev 3 :date "2022-10-19"}] {:temporal-period "1"})))))
