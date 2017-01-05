;;; Copyright (C) 2014 Adam Tornhill
;;;

(ns code-maat.app.grouper-test
  (:require [code-maat.app.grouper :as g]
            [incanter.core :as incanter])
  (:use clojure.test))

(def ^:const multi-group-spec
"/some/path => G1
/another/path => G2")

(def ^:const multi-regexp-group-spec
"^/some/path_\\w+_group1$ => G1
^/another/path_\\w+_group2$ => G2")

(def ^:const multi-mixed-group-spec
"/some/path => G1
^/another/path/\\.*$ => G2")

(deftest parses-specification
  (testing "Single group"
    (let [result (g/text->group-specification "/some/path => G1")]
     (is (= (print-str (get (nth result 0) :path)) "#\"^/some/path/\""))
     (is (= (get (nth result 0) :name) "G1"))))

  (testing "Multiple text groups"
    (let [result (g/text->group-specification multi-group-spec)]
     (is (= (print-str (get (nth result 0) :path)) "#\"^/some/path/\""))
     (is (= (get (nth result 0) :name) "G1"))
     (is (= (print-str (get (nth result 1) :path)) "#\"^/another/path/\""))
     (is (= (get (nth result 1) :name) "G2"))))

  (testing "Multiple regexp groups"
    (let [result (g/text->group-specification multi-regexp-group-spec)]
      (is (= (print-str (get (nth result 0) :path)) "#\"^/some/path_\\w+_group1$\""))
      (is (= (get (nth result 0) :name) "G1"))
      (is (= (print-str (get (nth result 1) :path)) "#\"^/another/path_\\w+_group2$\""))
      (is (= (get (nth result 1) :name) "G2"))))

  (testing "Multiple text and regexp groups"
    (let [result (g/text->group-specification multi-mixed-group-spec)]
      (is (= (print-str (get (nth result 0) :path)) "#\"^/some/path/\""))
      (is (= (get (nth result 0) :name) "G1"))
      (is (= (print-str (get (nth result 1) :path)) "#\"^/another/path/\\.*$\""))
      (is (= (get (nth result 1) :name) "G2"))))

  (testing "No groups"
    (is (= (g/text->group-specification "")
           [])))

  (testing "With backslash"
    (let [result (g/text->group-specification "/some\\\\path => G1")]
     (is (= (print-str (get (nth result 0) :path)) "#\"^/some\\\\path/\""))
     (is (= (get (nth result 0) :name) "G1"))))

   (testing "With dot in filename"
    (let [result (g/text->group-specification "/some/path/with.dot => G1")]
      (is (= (print-str (get (nth result 0) :path)) "#\"^/some/path/with.dot/\""))
      (is (= (get (nth result 0) :name) "G1"))))

   (testing "With dash in filename"
    (let [result (g/text->group-specification "/some/path/with-dash/x => G1")]
      (is (= (print-str (get (nth result 0) :path)) "#\"^/some/path/with-dash/x/\""))
      (is (= (get (nth result 0) :name) "G1")))))

(def ^:const entities-in-same-layer [{:entity "Top/A" :rev 1}
                                     {:entity "Top/B" :rev 2}])

(def ^:const entities-multiple-layers [{:entity "Top/A" :rev 1}
                                       {:entity "Bottom/B" :rev 2}])

(def ^:const entities-in-layers [{:entity "Layer/A/Entity" :rev 1}
                                 {:entity "Layer/B/Entity" :rev 2}])

(def as-ds incanter/to-dataset)

;; Specify the path to match a physical layer together with the name
;; of the logical layer:
(def ^:const top-level-layer [{:path #"^Top/" :name "T"}])
(def ^:const multiple-layers [{:path #"^Top/" :name "Top"} {:path #"^Bottom/" :name "infrastructure"}])
(def ^:const regex-one-layer [{:path #"^.*/A/.*$" :name "A Entities"}])
(def ^:const regex-same-layers [{:path #"^.*/Entity$" :name "All Entities"}])
(def ^:const regex-multiple-layers
  [{:path #"^.*/A/.*$" :name "A Entities"}
   {:path #"^.*/B/.*$" :name "B Entities"}])

(deftest entities-are-mapped-to-defined-layers
  (testing "Mapped to the same layer"
    (is (= (g/map-entities->groups entities-in-same-layer top-level-layer)
           [{:rev 1 :entity "T"}
            {:rev 2 :entity "T"}])))

  (testing "Mapped to different layers"
     (is (= (g/map-entities->groups entities-multiple-layers multiple-layers)
            [{:rev 1 :entity "Top"}
             {:rev 2 :entity "infrastructure"}])))

  (testing "Mapped via regex to the same layer"
    (is (= (g/map-entities->groups entities-in-layers regex-same-layers)
           [{:rev 1 :entity "All Entities"}
            {:rev 2 :entity "All Entities"}])))

  (testing "Mapped via regex to different layers"
     (is (= (g/map-entities->groups entities-in-layers regex-multiple-layers)
            [{:rev 1 :entity "A Entities"}
             {:rev 2 :entity "B Entities"}]))))

;; Filter out any entity that doesn't match the given layer structure.
;; Most of the time this is probably what we want.
(deftest unmapped-entities-are-ignored
  (is (= (g/map-entities->groups entities-multiple-layers top-level-layer)
         [{:rev 1 :entity "T"}]))
  (is (= (g/map-entities->groups entities-in-layers regex-one-layer)
         [{:rev 1 :entity "A Entities"}])))
