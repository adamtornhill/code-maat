(ns code-maat.app.team-mapper
  (:require [semantic-csv.core :as sc]))

(defn file->author-team-lookup
  "Parses a given team mapping expect to be a CSV with the columns author,team"
  [f]
  (->> f
       sc/slurp-csv
       (map (juxt :author :team))
       (into {})))

(defn- author->team
  [team-lookup {:keys [author] :as commit}]
  (assoc commit :author (get team-lookup author author)))

(defn run
  "Maps individual authors to teams as defined by team-lookup,
   which is expected to be a map from author to team (strings).
   Any author that isn't included in that mapping is
   simply kept as-is (see them as a team in themselves).
   This has the advantage that any omissions in the mapping are
   detected fast."
  [commits team-lookup]
  (map (partial author->team team-lookup) commits))

