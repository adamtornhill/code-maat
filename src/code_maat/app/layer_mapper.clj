;;; Copyright (C) 2014 Adam Tornhill
;;;

(ns code-maat.app.layer-mapper
  (:require [instaparse.core :as insta]))

;;; Code Maat supports analysis according to pre-defined layers.
;;; These layers are typically architectural boundaries. All data
;;; will be aggregated into that layered view before analysis.

;; Parsing the group specification
;; ===============================

(def ^:const group-grammar
  "Here's the instaparse grammar for a grouping/layering.
   We expect the groups to be specified as:
     some/path => some_name
   That is, everything on some/path will be grouped
   under some_name."
  "
    <S>       = groups
    <groups>  = (group <nl*>)* | group
    group     = path <ws+> <separator> <ws+> name
    path      = #'^[\\w/\\\\]+'
    separator = '=>'
    name      = #'[^\\n]+'
    ws        =  #'\\s'
    nl        =  '\\n'")

(defn- raise-parse-failure
  [f]
  (let [reason (with-out-str (print f))]
    (throw (IllegalArgumentException.
            (str "Invalid group specification: " reason)))))

(defn- as-grammar-map
  "The actual invokation of the parser.
   Returns a Hiccup parse tree upon success,
   otherwise an informative exception is thrown."
  [parser input]
  (let [result (insta/parse parser input)]
    (if (insta/failure? result)
      (raise-parse-failure (insta/get-failure result))
      result)))

;;; The parse result from instaparse is given as hiccup vectors.
;;; We define a set of accessors encapsulating the access to
;;; the individual parts of the associative vectors.

(defn- as-path
  [v]
  (get-in v [1 1]))

(defn- as-name
  [v]
  (get-in v [2 1]))

(defn- as-group-specification
  [v]
  {:path (as-path v)
   :name (as-name v)})

(defn text->group-specification
  "Transforms the given text into a
   seq of maps specifying the grouping.
   Each map will have the following content:
    {:path 'some/path' :name 'some_name'}" 
  [input]
  (let [parser (insta/parser group-grammar)]
    (->>
     input
     (as-grammar-map parser)
     (map as-group-specification))))

;; Mapping physical entities to logical groups
;; ===========================================

(defn- entity->logical-name
  [entity layer-exprs]
  (some (fn [{:keys [path-match logical-name]}]
          (when (re-find path-match entity)
            logical-name))
        layer-exprs))

(defn- commit->commit-by-layer
  [commit layer-exprs]
  (update-in commit [:entity] #(entity->logical-name % layer-exprs))) 

(defn- within-layers?
  [layer-exprs entity]
  (->>
   (map :path-match layer-exprs)
   (some #(re-find % entity))))

(defn- as-layer-expr
  [{:keys [path name]}]
  {:path-match (re-pattern (str "^" path "/"))
   :logical-name name})

(defn- as-layer-exprs
  [layers]
  (map as-layer-expr layers))

(defn map-entities->layers
  "Maps each entity in the commits to one of the pre-defined
   architectural layers.
   The layers are given as a seq of maps. Each map denotes the
   physical path to the layer together with its logical name.
   We translate that path into a regex that matches
   entity names with logical names."
  [commits layers]
  (let [layer-exprs (as-layer-exprs layers)]
    (->>
     (filter #(within-layers? layer-exprs (:entity %)) commits)
     (map #(commit->commit-by-layer % layer-exprs)))))

(defn- layers-from
  [layer-info-file]
  (slurp layer-info-file))

(defn run
  "This entry point parses the given layer info.
   All entities in each commit are then re-mapped to one
   of the given layers "
  [layer-info-file commits]
  (->>
   (layers-from layer-info-file)
   text->group-specification
   (map-entities->layers commits)))
