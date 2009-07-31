;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: Jul 30, 2009

;;(set! *warn-on-reflection* true)

(ns clojure.benchmark.reverse-complement
  (:use [clojure.contrib.seq-utils :only (flatten)]))


(defn println-string-to-buffered-writer [#^java.io.BufferedWriter bw
					 #^java.lang.String s]
  (. bw write (.toCharArray s) 0 (count s))
  (. bw newLine))


(let [br (java.io.BufferedReader. *in*)
      bw (java.io.BufferedWriter. *out*)]
  (doseq [ln (reverse (line-seq br))]
    (println-string-to-buffered-writer bw ln))
  (. bw flush))


(. System (exit 0))
