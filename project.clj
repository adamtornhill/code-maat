;;; Copyright (C) 2013-2015 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(defproject code-maat "0.8.6"
  :description "A toolset to mine and analyze version control data"
  :url "http://www.adamtornhill.com/code/codemaat.htm"
  :license {:name "GNU General Public License v3.0"
            :url "http://www.gnu.org/licenses/gpl.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.zip "0.1.1"]
		 [incanter/incanter-core "1.5.6"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/data.csv "0.1.2"]
                 [clj-time "0.9.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/math.combinatorics "0.0.8"]
                 [instaparse "1.3.5"]]
  :main code-maat.cmd-line
  :aot [code-maat.cmd-line]
  :jvm-opts ["-Xmx4g" "-Djava.awt.headless=true" "-Xss512M"])
