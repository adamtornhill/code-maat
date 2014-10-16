;;; Copyright (C) 2014 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.analysis.commit-messages-test
  (:require [code-maat.analysis.commit-messages :as c]
            [code-maat.analysis.test-data :as td]
             [code-maat.dataset.dataset :as ds])
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
  
