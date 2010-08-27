;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: Jul 29, 2009

(ns revcomp
  (:gen-class))

(set! *warn-on-reflection* true)

(defn fasta-description-line
  "Return true when the line l is a FASTA description line"
  [l]
  (= \> (first (seq l))))


;; TBD: Try avoiding the use of when-let, in case it might be causing
;; me to hold on to a head of a sequence when I don't want to.

(defn fasta-desc-dna-str-pairs
  "Take a sequence of lines from a FASTA format file.  Return a lazy sequence of 2-element vectors [desc dna-seq], where desc is a FASTA description line, and sdna-seq is the concatenated FASTA DNA sequence with that description.  If input file is big, you'll save lots of memory if you call this function in a with-open for the file, and don't hold on to the head of the lines parameter."
  [lines]
  (lazy-seq
    (let [lines (seq (drop-while (fn [l] (not (fasta-description-line l)))
				 lines))]
      (when lines
	(let [[lines-before-next-desc next-desc-line-onwards]
	      (split-with (fn [l] (not (fasta-description-line l)))
			  (rest lines))]
	  (cons [(first lines) (apply str lines-before-next-desc)]
		(fasta-desc-dna-str-pairs next-desc-line-onwards)))))))


(comment

(defn fasta-desc-dna-str-pairs
  "Take a sequence of lines from a FASTA format file.  Return a lazy sequence of 2-element vectors [desc dna-seq], where desc is a FASTA description line, and sdna-seq is the concatenated FASTA DNA sequence with that description.  If input file is big, you'll save lots of memory if you call this function in a with-open for the file, and don't hold on to the head of the lines parameter."
  [lines]
  (lazy-seq
    (when-let [lines (drop-while (fn [l]
				   (not (fasta-description-line l)))
				 lines)]
      (when-let [lines (seq lines)]
	(let [[lines-before-next-desc next-desc-line-onwards]
	      (split-with (fn [l] (not (fasta-description-line l)))
			  (rest lines))]
	  (cons [(first lines) (apply str lines-before-next-desc)]
		(fasta-desc-dna-str-pairs next-desc-line-onwards)))))))
)


(def complement-of-dna-char
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


(defn print-reverse-complement-of-str-in-lines [#^java.io.BufferedWriter bw
						#^java.lang.String s
						max-len]
  (let [comp complement-of-dna-char
	len (int (count s))
	max-len (int max-len)]
    (when (> len 0)
      (loop [start (int (dec len))
	     to-print-before-nl (int max-len)]
	(let [next-start (int (dec start))
	      next-to-print-before-nl (int (dec to-print-before-nl))]
	  (. bw write (int (comp (. s charAt start))))
	  (when (zero? next-to-print-before-nl)
	    (. bw newLine))
	  (when (not (zero? start))
	    (if (zero? next-to-print-before-nl)
	      (recur next-start max-len)
	      (recur next-start next-to-print-before-nl)))))
      ;; Need one more newline at the end if the string was not a
      ;; multiple of max-len characters.
      (when (not= 0 (rem len max-len))
	(. bw newLine))
      )))


(defn println-string-to-buffered-writer [#^java.io.BufferedWriter bw
					 #^java.lang.String s]
  (. bw write (.toCharArray s) 0 (count s))
  (. bw newLine))


(defn -main [& args]
  (let [max-dna-chars-per-line 60]
;;  (with-open [br (java.io.BufferedReader. *in*)
;;	      bw (java.io.BufferedWriter. *out*)]
    (let [br (java.io.BufferedReader. *in*)
          bw (java.io.BufferedWriter. *out*)]
      (doseq [[desc dna-seq] (fasta-desc-dna-str-pairs (line-seq br))]
        (println-string-to-buffered-writer bw desc)
        (println-string-to-buffered-writer bw dna-seq)
;;      (print-reverse-complement-of-str-in-lines bw dna-seq
;;						max-dna-chars-per-line)
        (. bw flush))
      ))
  (. System (exit 0)))


(comment

(let [max-dna-chars-per-line 60]
;;  (with-open [br (java.io.BufferedReader. *in*)
;;	      bw (java.io.BufferedWriter. *out*)]
  (let [br (java.io.BufferedReader. *in*)
	bw (java.io.BufferedWriter. *out*)]
    (doseq [[dna-seq-num [desc dna-seq]]
	    (map (fn [x y] [x y])
		 (iterate inc 1)
		 (fasta-desc-dna-str-pairs (line-seq br)))]
      (println-string-to-buffered-writer bw (format "%d" dna-seq-num))
      (println-string-to-buffered-writer bw desc)
      ;; (. bw flush)
      (print-reverse-complement-of-str-in-lines bw dna-seq
						max-dna-chars-per-line)
      ;; (. bw flush)
      )
    (. bw flush)
    ))
)
