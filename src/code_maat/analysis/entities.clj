(ns code-maat.analysis.entities
  (:use incanter.core))

;;; TODO: differ between new and modified? Have to be tagged during parsing!
(defn all [ds]
  (set ($ :entity ds)))