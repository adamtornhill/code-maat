(ns code-maat.app.cmd-line-test
  (:require  [clojure.test :refer :all]
             [clojure.tools.cli :as cli]
             [code-maat.cmd-line :refer :all]))


(deftest test-argument-parsing
  (testing "simple cmd line parsing"
    (let [args ["-l some_file.log"]
          parsed-options (cli/parse-opts args cli-options)]

      (is (nil? (:errors parsed-options))))))
