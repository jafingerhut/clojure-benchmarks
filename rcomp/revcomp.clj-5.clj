;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: Jul 29, 2009

;;(set! *warn-on-reflection* true)

(ns clojure.benchmark.reverse-complement
  (:use [clojure.contrib.seq-utils :only (flatten)]))


(defn fasta-description-line
  "Return true when the line l is a FASTA description line"
  [l]
  (= \> (first (seq l))))


;;(defn split-with-2
;;  [pred coll]
;;  (loop [s (seq coll)
;;	 reversed-take-while '()]
;;    (if s
;;      (if (pred (first s))
;;	(recur (next s) (cons (first s) reversed-take-while))
;;	[(reverse reversed-take-while) s])
;;      [(reverse reversed-take-while) '()])))


(defn fasta-desc-dna-str-pairs
  "Take a sequence of lines from a FASTA format file.  Return a lazy sequence of 2-element vectors [desc dna-seq], where desc is a FASTA description line, and sdna-seq is a sequence of strings, each one line of the FASTA DNA sequence from the input file with that description.  Callers can potentially save lots of memory by not holding on to a reference to the head of the lines parameter."
  [lines]
  (lazy-seq
    (let [lines (seq (drop-while (fn [l] (not (fasta-description-line l)))
				 lines))]
      (when lines
	(let [[lines-before-next-desc next-desc-line-onwards]
	      (split-with (fn [l] (not (fasta-description-line l)))
			  (rest lines))]
	  (cons [(first lines) lines-before-next-desc]
		(fasta-desc-dna-str-pairs next-desc-line-onwards)))))))


(def complement-dna-char-map
     {\w \W, \W \W,
      \s \S, \S \S,
      \a \T, \A \T,
      \t \A, \T \A,
      \u \A, \U \A,
      \g \C, \G \C,
      \c \G, \C \G,
      \y \R, \Y \R,
      \r \Y, \R \Y,
      \k \M, \K \M,
      \m \K, \M \K,
      \b \V, \B \V,
      \d \H, \D \H,
      \h \D, \H \D,
      \v \B, \V \B,
      \n \N, \N \N })


(defn make-vec-char-mapper [cmap]
  (into [] (map (fn [code]
		  (if (contains? cmap (char code))
		    (cmap (char code))
		    (char code)))
		(range 256))))


;;(defn print-char-seq-with-line-breaks
;;  [#^java.io.BufferedWriter bw char-seq max-line-len]
;;  (let [max-line-len (int max-line-len)
;;	do-write (fn []
;;		   (loop [char-seq (seq char-seq)
;;			  to-print-before-nl (int max-line-len)]
;;		     (if char-seq
;;		       (let [next-to-print-before-nl (int (dec to-print-before-nl))]
;;			 (. bw write (int (first char-seq)))
;;			 (when (zero? next-to-print-before-nl)
;;			   (. bw newLine))
;;			 (if (zero? next-to-print-before-nl)
;;			   (recur (next char-seq) max-line-len)
;;			   (recur (next char-seq) next-to-print-before-nl)))
;;		       to-print-before-nl)))]
;;    (when (not= max-line-len (do-write))
;;      (. bw newLine))))


(defn print-char-seq-with-line-breaks
  [#^java.io.BufferedWriter bw char-seq max-line-len]
  (let [max-line-len (int max-line-len)]
    (loop [char-seq (seq char-seq)
	   to-print-before-nl (int max-line-len)]
      (if char-seq
	(let [next-to-print-before-nl (int (dec to-print-before-nl))]
	  (. bw write (int (first char-seq)))
	  (when (zero? next-to-print-before-nl)
	    (. bw newLine))
	  (if (zero? next-to-print-before-nl)
	    (recur (next char-seq) max-line-len)
	    (recur (next char-seq) next-to-print-before-nl)))
	(when (not= to-print-before-nl max-line-len)
	  (. bw newLine))))))


(defn reverse-complement-of-str-seq [str-seq complement-fn]
  (map complement-fn (flatten (map reverse (reverse str-seq)))))


(defn println-string-to-buffered-writer [#^java.io.BufferedWriter bw
					 #^java.lang.String s]
  (. bw write (.toCharArray s) 0 (count s))
  (. bw newLine))


(let [max-dna-chars-per-line 60
      br (java.io.BufferedReader. *in*)
      bw (java.io.BufferedWriter. *out*)
      ;; We could use the map complement-dna-char-map instead of
      ;; complement-dna-char-fn, but when I tested that, the program
      ;; spent a lot of time running the hashCode method on
      ;; characters.  I'm hoping this is faster.
      complement-dna-char-vec (make-vec-char-mapper complement-dna-char-map)
      complement-dna-char-fn (fn [ch] (complement-dna-char-vec (int ch)))]
  (doseq [[desc str-seq] (fasta-desc-dna-str-pairs (line-seq br))]
    (println-string-to-buffered-writer bw desc)
    (print-char-seq-with-line-breaks bw
      (reverse-complement-of-str-seq str-seq complement-dna-char-fn)
      max-dna-chars-per-line))
  (. bw flush))


(. System (exit 0))
