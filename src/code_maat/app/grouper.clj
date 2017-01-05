;;; Copyright (C) 2014 Adam Tornhill
;;;

(ns code-maat.app.grouper
  (:require [instaparse.core :as insta])
  (:require [clojure.string :as string]))

;;; Code Maat supports analysis according to pre-defined architectual groups.
;;; These groups are typically architectural boundaries. All data
;;; will be aggregated into that view before analysis.

;; Parsing the group specification
;; ===============================

(def ^:const group-grammar
  "Here's the instaparse grammar for a regex-based grouping/layering.
   We expect the groups to be specified as text:
      some-path => some_name
  or with a regex:
     ^some-regexp$ => some_name
   That is, everything matching some-path or some-regexp will be grouped
   under some_name."
  "
    <S>       = groups
    <groups>  = (group <nl*>)* | group
    group     = path <ws+> <separator> <ws+> name
    path      = #'((^[\\w/\\\\\\.\\-]+)|(\\^.+\\$))'
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
  (let [p (as-path v)]
    {:path (if (string/starts-with? p "^")
              (re-pattern (str p))
              (re-pattern (str "^" p "/")))
    :name (as-name v)}))

(defn text->group-specification
  "Transforms the given text or regular expression into a
   seq of maps specifying the grouping.
   Each map will have a path key corresponding to a regex pattern
   and a name key corresponding to the grouping logical name:
    {:path #'^some/path/' :name 'some_name'}
    or
    {:path #'^some-regexp$' :name 'some_name'}"
  [input]
  (let [parser (insta/parser group-grammar)]
    (->>
     input
     (as-grammar-map parser)
     (map as-group-specification))))

;; Mapping physical entities to logical groups
;; ===========================================

(defn- entity->logical-name
  [entity group-exprs]
  (some (fn [{:keys [path-match logical-name]}]
          (when (re-find path-match entity)
            logical-name))
        group-exprs))

(defn- commit->commit-by-group
  [commit group-exprs]
  (update-in commit [:entity] #(entity->logical-name % group-exprs)))

(defn- within-group?
  [group-exprs entity]
  (->>
   (map :path-match group-exprs)
   (some #(re-find % entity))))

(defn- as-group-expr
  [{:keys [path name]}]
  {:path-match path :logical-name name})

(defn- as-group-exprs
  [group-spec]
  (map as-group-expr group-spec))

(defn map-entities->groups
  "Maps each entity in the commits to one of the pre-defined
   architectural boundaries (groups).
   The groups are given as a seq of maps. Each map denotes the
   physical path to the group together with its logical name.
   We translate that path into a regex that matches
   entity names with logical names."
  [commits groups]
  (let [group-exprs (as-group-exprs groups)]
    (->>
     (filter #(within-group? group-exprs (:entity %)) commits)
     (map #(commit->commit-by-group % group-exprs)))))

(defn- groups-from
  [group-info-file]
  (slurp group-info-file))

(defn run
  "This entry point parses the given group info.
   All entities in each commit are then re-mapped to one
   of the given groups."
  [group-info-file commits]
  (->>
   (groups-from group-info-file)
   text->group-specification
   (map-entities->groups commits)))
