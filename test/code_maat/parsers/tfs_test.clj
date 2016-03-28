;;; Copyright (C) 2016 Ryan Coy
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.tfs_test
  (:require [code-maat.parsers.tfs :as tfs])
  (:use clojure.test incanter.core))

(def ^:const en-us-entry
  "-----------------------------------------------------------------------------------------------------------------------
Changeset: 5
User: Ryan Coy
Date: Thursday, July 23, 2015 4:34:31 PM

Comment:
  Created team project folder /Project via the Team Project Creation Wizard

Items:
  add $/Project

")

(def ^:const checkin-notes-entry
  "-----------------------------------------------------------------------------------------------------------------------
Changeset: 5
User: Ryan Coy
Date: Thursday, July 23, 2015 4:34:31 PM

Comment:
  Created team project folder /Project via the Team Project Creation Wizard

Items:
  add $/Project

Check-in Notes:
  Documentation:
    An important new part of our codebase.
")

(def ^:const long-comment-entry
  "-----------------------------------------------------------------------------------------------------------------------
Changeset: 5
User: Ryan Coy
Date: Thursday, July 23, 2015 4:34:31 PM

Comment:
  Created team project folder /Project via the Team Project Creation Wizard
  Gave project a unique and colorful name

  It really is the best project.
  ***NO_CI***

Items:
  add $/Project

")

(def ^:const proxy-checkin-entry
  "-----------------------------------------------------------------------------------------------------------------------
Changeset: 5
User: Ryan Coy
Checked in by: build.server
Date: Thursday, July 23, 2015 4:34:31 PM

Comment:
  Created team project folder /Project via the Team Project Creation Wizard

Items:
  add $/Project

")

(def ^:const policy-warning-entry
  "-----------------------------------------------------------------------------------------------------------------------
Changeset: 5
User: Ryan Coy
Date: Thursday, July 23, 2015 4:34:31 PM

Comment:
  Created team project folder /Project via the Team Project Creation Wizard

Items:
  add $/Project

Policy Warnings:
  Override Reason:
    We don't need no comments

    Not at all
  Messages:
    Provide a comment for the check-in.

    ...or Else
")

(def ^:const en-gb-entry
  "-----------------------------------------------------------------------------------------------------------------------
Changeset: 5
User: Ryan Coy
Date: 23 July 2015 16:34:31

Comment:
  Created team project folder /Project via the Team Project Creation Wizard

Items:
  add $/Project

")

(def ^:const entries
  "-----------------------------------------------------------------------------------------------------------------------
Changeset: 7
User: Ryan Coy
Date: Thursday, July 23, 2015 4:34:35 PM

Comment:
  Check-in the Lab default template

Items:
  add $/Project/BuildProcessTemplates/LabDefaultTemplate.11.xaml

-----------------------------------------------------------------------------------------------------------------------
Changeset: 6
User: Ryan Coy
Date: Thursday, July 23, 2015 4:34:34 PM

Comment:
  Checking in new Team Foundation Build Automation files.

Items:
  add $/Project/BuildProcessTemplates
  add $/Project/BuildProcessTemplates/AzureContinuousDeployment.11.xaml
  add $/Project/BuildProcessTemplates/DefaultTemplate.11.1.xaml
  add $/Project/BuildProcessTemplates/UpgradeTemplate.xaml

-----------------------------------------------------------------------------------------------------------------------
Changeset: 5
User: Coy, Ryan
Date: Thursday, July 23, 2015 4:34:31 PM

Comment:
  Created team project folder /Project via the Team Project Creation Wizard

Items:
  add $/Project

")

(defn- parse
  [text]
  (tfs/parse-read-log text {}))

(deftest parses-en-us-entry-to-dataset
  (is (= (parse en-us-entry)
         [{:author "Ryan Coy"
           :rev "5"
           :date "2015-07-23"
           :entity "/Project"
           :message "Created team project folder /Project via the Team Project Creation Wizard"}])))

(deftest parses-checkin-notes-to-dataset
  (is (= (parse checkin-notes-entry)
         [{:author "Ryan Coy"
           :rev "5"
           :date "2015-07-23"
           :entity "/Project"
           :message "Created team project folder /Project via the Team Project Creation Wizard"}])))

(deftest parses-policy-warning-to-dataset
  (is (= (parse policy-warning-entry)
         [{:author "Ryan Coy"
           :rev "5"
           :date "2015-07-23"
           :entity "/Project"
           :message "Created team project folder /Project via the Team Project Creation Wizard"}])))

(deftest parses-long-comment-to-dataset
  (is (= (parse long-comment-entry)
         [{:author "Ryan Coy"
           :rev "5"
           :date "2015-07-23"
           :entity "/Project"
           :message "Created team project folder /Project via the Team Project Creation Wizard\nGave project a unique and colorful name\nIt really is the best project.\n***NO_CI***"}])))

(deftest parses-proxy-checkin-to-dataset
  (is (= (parse proxy-checkin-entry)
         [{:author "Ryan Coy"
           :rev "5"
           :date "2015-07-23"
           :entity "/Project"
           :message "Created team project folder /Project via the Team Project Creation Wizard"}])))

(deftest unparsable-date-throws-exception
  (is (thrown? IllegalArgumentException
        (parse en-gb-entry))))

(deftest parses-multiple-entries-to-dataset
  (is (= (parse entries)
         [{:author "Ryan Coy"
           :rev "7"
           :date "2015-07-23"
           :entity "/Project/BuildProcessTemplates/LabDefaultTemplate.11.xaml"
           :message "Check-in the Lab default template"}
          {:author "Ryan Coy"
           :rev "6"
           :date "2015-07-23"
           :entity "/Project/BuildProcessTemplates"
           :message "Checking in new Team Foundation Build Automation files."}
          {:author "Ryan Coy"
           :rev "6"
           :date "2015-07-23"
           :entity "/Project/BuildProcessTemplates/AzureContinuousDeployment.11.xaml"
           :message "Checking in new Team Foundation Build Automation files."}
          {:author "Ryan Coy"
           :rev "6"
           :date "2015-07-23"
           :entity "/Project/BuildProcessTemplates/DefaultTemplate.11.1.xaml"
           :message "Checking in new Team Foundation Build Automation files."}
          {:author "Ryan Coy"
           :rev "6"
           :date "2015-07-23"
           :entity "/Project/BuildProcessTemplates/UpgradeTemplate.xaml"
           :message "Checking in new Team Foundation Build Automation files."}
          {:author "Coy, Ryan"
           :rev "5"
           :date "2015-07-23"
           :entity "/Project"
           :message "Created team project folder /Project via the Team Project Creation Wizard"}])))

(deftest parses-empty-log-to-empty-dataset
  (is (= (parse "")
         [])))

