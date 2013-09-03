;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.test.data-driven
  (:use clojure.test))

(defmacro def-dd-test
  "Defines a data-driven test based on clojure.test/deftest.
   Note that this macro is more of a starting point solving
   my immediate needs than a good idea.

   Example:
     (def-dd-test gittest
          [ddval [git svn]]
          (is (= (analysis ddval)
                 expected-output)))

   The code above will generate two deftest, one for each supplied value.
   The generated code in each deftest will evaluate the body (is (= ...)) with the
    symbol ddval bound to 'git' for the first deftest and 'svn' for the second deftest."
  [name args & body]
  (let [[param values] args]
    `(do
       ~@(for [i (range (count values))]
           (let [value (nth values i)
                 name (symbol (str name "-" value))] 
             `(deftest ~name
                (let [~param ~value]
                  ~@body)))))))
