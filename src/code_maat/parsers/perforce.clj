(ns
  ^{:author robertc}
  code-maat.parsers.perforce
  (:require [instaparse.core :as insta]
            [code-maat.parsers.hiccup-based-parser :as hbp]
            [incanter.core :as incanter]))

(def ^:const perforce-grammar
  "Here's the instaparse grammar for a perforce log-file.
   In the current version we only extract basic info on
   authors and file modification patterns.
   To calculate churn, we parse the lines added/deleted too.
   That info is added b the numstat argument."
  "
   <S>       =   entries
   <entries> =  (entry <nl>)* | entry
   entry     =  rev <ws> author <ws> date <nl+> <message> <header> changes
   rev       =  <'Change'> <ws> #'[\\d]+'
   author    =  <'by'> <ws> #'[^@]+' <#'[^\\s]+'>
   date      =  <'on'> <ws> #'\\d{4}/\\d{2}/\\d{2}' <ws> <#'\\d{2}:\\d{2}:\\d{2}'>
   message   =  (tab ws* #'.+' nl)+
   header    =  nl 'Affected files ...' nl nl
   changes   =  (file <nl?>)+
   file      =  <'... '> #'[^#]+' <#'.+'>
   ws        =  #'\\s'
   tab       =  #'\\t'
   nl        =  #'(\\r)?\\n'
   ")

(def positional-extractors
  "Specify a set of functions to extract the parsed values."
  {:rev #(get-in % [1 1])
   :author #(get-in % [2 1])
   :date #(get-in % [3 1])
   :message (fn [_] "")
   :changes #(rest (get-in % [4]))
 })

(defn parse-log
  "Transforms the given input perforce log into an
   Incanter dataset suitable for the analysis modules."
  [input parse-options]
  (hbp/parse-log input perforce-grammar parse-options positional-extractors))
