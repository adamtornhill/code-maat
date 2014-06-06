;;; Copyright (C) 2014 Adam Tornhill
;;;

(ns code-maat.app.layer-mapper)

;;; Code Maat supports analysis according to pre-defined layers.
;;; These layers are typically architectural boundaries. All data
;;; will be aggregated into that layered view before analysis.

(defn- entity->layer
  [entity layer-exprs]
  (some #(re-find % entity) layer-exprs))

(defn- commit->commit-by-layer
  [commit layer-exprs]
  (update-in commit [:entity] #(entity->layer % layer-exprs))) 

(defn- within-layers?
  [layer-exprs entity]
  (some #(re-find % entity) layer-exprs))

(defn- as-layer-exprs
  [layers]
  (map #(re-pattern (str "^" % "/")) layers))

(defn map-entities->layers
  "Maps each entity in the commits to one of the pre-defined
   architectural layers.
   The layers are given as a seq of strings. Each string denotes the
   name of a layer. We translate that string into a regex that matches
   entity names with layer names."
  [commits layers]
  (let [layer-exprs (as-layer-exprs layers)]
    (->>
     (filter #(within-layers? layer-exprs (:entity %)) commits)
     (map #(commit->commit-by-layer % layer-exprs)))))

(defn- layers-from
  [layer-info-file]
  (with-open [rdr (clojure.java.io/reader layer-info-file)]
    (doall (line-seq rdr))))

(defn run
  "This entry point parses the given layer info.
   All entities in each commit are then re-mapped to one
   of the given layers "
  [layer-info-file commits]
  (->>
   (layers-from layer-info-file)
   (map-entities->layers commits)))
