;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(defproject code-maat "0.4.0-SNAPSHOT"
  :description "A toolset to mine and analyze version control data"
  :url "http://http://www.adampetersen.se/code/codemaat.htm"
  :license {:name "GNU General Public License v3.0"
            :url "http://www.gnu.org/licenses/gpl.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.zip "0.1.0"]
		 [incanter "1.5.2"]
                 [org.clojure/tools.cli "0.2.4"]
                 [org.clojure/data.csv "0.1.2"]
                 [clj-time "0.5.1"]
                 [org.clojure/math.numeric-tower "0.0.2"]
                 [org.clojure/math.combinatorics "0.0.4"]
                 [instaparse "1.2.2"]]
  :main code-maat.core
  :aot [code-maat.core]
  :jvm-opts ["-Xmx4g" "-Djava.awt.headless=true"])
