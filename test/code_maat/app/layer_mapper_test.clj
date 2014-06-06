;;; Copyright (C) 2014 Adam Tornhill
;;;

(ns code-maat.app.layer-mapper-test
  (:require [code-maat.app.layer-mapper :as mapper]
            [incanter.core :as incanter])
  (:use clojure.test))

(def ^:const entities-in-same-layer [{:entity "Top/A" :rev 1}
                                     {:entity "Top/B" :rev 2}])

(def ^:const entities-multiple-layers [{:entity "Top/A" :rev 1}
                                       {:entity "Bottom/B" :rev 2}])

(def as-ds incanter/to-dataset)

(def ^:const top-level-layer ["Top"])
(def ^:const multiple-layers ["Top" "Bottom"])

(deftest entities-are-mapped-to-defined-layers
  (testing "Mapped to the same layer"
    (is (= (mapper/map-entities->layers entities-in-same-layer top-level-layer)
           [{:rev 1 :entity "Top/"}
            {:rev 2 :entity "Top/"}])))
  (testing "Mapped to different layers"
     (is (= (mapper/map-entities->layers entities-multiple-layers multiple-layers)
            [{:rev 1 :entity "Top/"}
             {:rev 2 :entity "Bottom/"}]))))

;; Filter out any entity that doesn't match the given layer structure.
;; Most of the time this is probably what we want.
(deftest unmapped-entities-are-ignored
  (is (= (mapper/map-entities->layers entities-multiple-layers top-level-layer)
         [{:rev 1 :entity "Top/"}])))
