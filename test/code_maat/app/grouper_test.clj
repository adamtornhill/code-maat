;;; Copyright (C) 2014 Adam Tornhill
;;;

(ns code-maat.app.grouper-test
  (:require [code-maat.app.grouper :as g]
            [incanter.core :as incanter])
  (:use clojure.test))

(def ^:const multi-group-spec
"/some/path => G1
/another/path => G2")

(deftest parses-specification
  (testing "Single group"
    (is (= (g/text->group-specification "/some/path => G1")
           [{:path "/some/path" :name "G1"}])))
  (testing "Multiple groups"
    (is (= (g/text->group-specification multi-group-spec)
           [{:path "/some/path" :name "G1"}
            {:path "/another/path" :name "G2"}])))
   (testing "No groups"
    (is (= (g/text->group-specification "")
           [])))
   (testing "With backslash"
    (is (= (g/text->group-specification "/some\\path => G1")
           [{:path "/some\\path" :name "G1"}])))
   (testing "With dot in filename"
     (is (= (g/text->group-specification "/some/path/with.dot => G1")
            [{:path "/some/path/with.dot" :name "G1"}])))
   (testing "With dash in filename"
     (is (= (g/text->group-specification "/some/path/with-dash/x => G1")
            [{:path "/some/path/with-dash/x" :name "G1"}]))))

(def ^:const entities-in-same-layer [{:entity "Top/A" :rev 1}
                                     {:entity "Top/B" :rev 2}])

(def ^:const entities-multiple-layers [{:entity "Top/A" :rev 1}
                                       {:entity "Bottom/B" :rev 2}])

(def as-ds incanter/to-dataset)

;; Specify the path to match a physical layer together with the name
;; of the logical layer:
(def ^:const top-level-layer [{:path "Top" :name "T"}])
(def ^:const multiple-layers [{:path "Top" :name "Top"} {:path "Bottom" :name "infrastructure"}])

(deftest entities-are-mapped-to-defined-layers
  (testing "Mapped to the same layer"
    (is (= (g/map-entities->groups entities-in-same-layer top-level-layer)
           [{:rev 1 :entity "T"}
            {:rev 2 :entity "T"}])))
  (testing "Mapped to different layers"
     (is (= (g/map-entities->groups entities-multiple-layers multiple-layers)
            [{:rev 1 :entity "Top"}
             {:rev 2 :entity "infrastructure"}]))))

;; Filter out any entity that doesn't match the given layer structure.
;; Most of the time this is probably what we want.
(deftest unmapped-entities-are-ignored
  (is (= (g/map-entities->groups entities-multiple-layers top-level-layer)
         [{:rev 1 :entity "T"}])))
