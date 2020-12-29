;;; Copyright (C) 2014 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.commit-messages-test
  (:require [code-maat.analysis.commit-messages :as c]
            [code-maat.analysis.test-data :as td]
            [code-maat.dataset.dataset :as ds]
            [incanter.core :as incanter])
  (:use clojure.test))

(defn- as-option
  [word]
  {:expression-to-match word})

(deftest identifies-matching-words
  (is (= (c/by-word-frequency td/vcsd (as-option "change"))
         (ds/-dataset [:entity :matches]
                      [["A" 3] ["B" 1]])))
  (is (= (c/by-word-frequency td/vcsd (as-option "Third"))
         (ds/-dataset [:entity :matches]
                      [["A" 1]])))
  (is (= (c/by-word-frequency td/vcsd (as-option "no match for this"))
         (ds/-dataset [:entity :matches]
                      []))))

(defn- run-messages-analysis
  [commits]
  (-> commits
      incanter/to-dataset
      (c/by-word-frequency (as-option "change"))))

(deftest detects-absent-message-fields ; not all supported version-control formats include a commit message
  (testing "No commit messages triggers exception"
    (is (thrown? IllegalArgumentException
                 (run-messages-analysis [{:author "apt" :entity "A" :rev 1 :message "-"}
                                         {:author "apt" :entity "B" :rev 2 :message "-"}]))))
  (testing "Empy dataset is valid"
    (is (= (run-messages-analysis [])
           (ds/-dataset [:entity :matches]
                        []))))
  (testing "Valid, if any commit has a message"
    (is (= (run-messages-analysis [{:author "apt" :entity "A" :rev 1 :message "-"}
                                   {:author "apt" :entity "B" :rev 2 :message "some change message"}])
           (ds/-dataset [:entity :matches]
                        [["B" 1]])))))

  
