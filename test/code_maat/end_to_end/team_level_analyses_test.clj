;;; Copyright (C) 2017 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.end-to-end.team-level-analyses-test
  (:require [code-maat.app.app :as app])
  (:use clojure.test))

(def ^:private git-log-file "./test/code_maat/end_to_end/mono_git.log")

(deftest runs-the-analyses-on-team-level
  (testing "Translates individuals to team according to the provided CSV file with mappings"
    (is (= (with-out-str
             (app/run git-log-file
                      {:version-control "git"
                       :analysis "main-dev"
                       :team-map-file "./test/code_maat/end_to_end/mono_git_team_map.csv"
                       :rows 3}))
           "entity,main-dev,added,total-added,ownership\neglib/src/gfile-posix.c,Another Team,1,1,1.0\nmcs/class/Microsoft.Build.Engine/Microsoft.Build.BuildEngine/Project.cs,Mono,4,4,1.0\nmcs/class/Microsoft.Build.Engine/Test/Microsoft.Build.BuildEngine/ProjectTest.cs,Mono,11,11,1.0\n")))
  (testing "Compare the above test to the same analysis on individual authors"
    (is (= (with-out-str
             (app/run git-log-file
                      {:version-control "git"
                       :analysis "main-dev"
                       :rows 3}))
           "entity,main-dev,added,total-added,ownership\neglib/src/gfile-posix.c,Rodrigo Kumpera,1,1,1.0\nmcs/class/Microsoft.Build.Engine/Microsoft.Build.BuildEngine/Project.cs,Lluis Sanchez,4,4,1.0\nmcs/class/Microsoft.Build.Engine/Test/Microsoft.Build.BuildEngine/ProjectTest.cs,Lluis Sanchez,11,11,1.0\n"))))

