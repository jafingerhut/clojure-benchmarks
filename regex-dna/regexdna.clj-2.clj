;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: Jul 30, 2009


;;(set! *warn-on-reflection* true)

(ns clojure.benchmark.regex-dna
  (:use [clojure.contrib.str-utils :only (re-gsub)])
  (:import (java.util.regex Pattern)))


;; Slightly modified from standard library slurp so that it can read
;; from standard input.

(defn slurp-std-input
  "Reads the standard input using the encoding enc into a string
  and returns it."
  ([] (slurp-std-input (.name (java.nio.charset.Charset/defaultCharset))))
  ([#^String enc]
     (with-open [r (new java.io.BufferedReader *in*)]
       (let [sb (new StringBuilder)]
	 (loop [c (.read r)]
	   (if (neg? c)
	     (str sb)
	     (do
	       (.append sb (char c))
	       (recur (.read r)))))))))


(def dna-seq-regexes '(    "agggtaaa|tttaccct"
		       "[cgt]gggtaaa|tttaccc[acg]"
		       "a[act]ggtaaa|tttacc[agt]t"
		       "ag[act]gtaaa|tttac[agt]ct"
		       "agg[act]taaa|ttta[agt]cct"
		       "aggg[acg]aaa|ttt[cgt]ccct"
		       "agggt[cgt]aa|tt[acg]accct"
		       "agggta[cgt]a|t[acg]taccct"
		       "agggtaa[cgt]|[acg]ttaccct" ))


(def iub-codes { "B"  "(c|g|t)",
		 "D"  "(a|g|t)",
		 "H"  "(a|c|t)",
		 "K"  "(g|t)",
		 "M"  "(a|c)",
		 "N"  "(a|c|g|t)",
		 "R"  "(a|g)",
		 "S"  "(c|g)",
		 "V"  "(a|c|g)",
		 "W"  "(a|t)",
		 "Y"  "(c|t)"     })


(defn do-all-replacements [s iub-codes]
  (let [re (. Pattern (compile (str "[" (apply str (keys iub-codes)) "]")))]
    (re-gsub re #(iub-codes %) s)))


(let [content (slurp-std-input)
      original-len (count content)
      ;; I'd prefer if I could use the regexp #"(^>.*)?\n" like the
      ;; Perl benchmark does, but that only matches ^ at the beginning
      ;; of the string, not at the beginning of a line in the middle
      ;; of the string.
      content (re-gsub #"(^>.*|\n>.*)?\n" "" content)
      dna-seq-only-len (count content)]

  (doseq [re dna-seq-regexes]
    (println (format "%s %d" re
		     ;; Prepending (?i) to the regexp in Java makes it
		     ;; case-insensitive.
		     (count (re-seq (. Pattern (compile (str "(?i)" re)))
				    content)))))

  ;; The problem description says that these replacements should be
  ;; done "one at a time", but it seems that several of the benchmarks
  ;; do them simultaneously.  Let me try that, too.
  (let [content (do-all-replacements content iub-codes)]
    (println (format "\n%d\n%d\n%d" original-len dna-seq-only-len
		     (count content)))))
