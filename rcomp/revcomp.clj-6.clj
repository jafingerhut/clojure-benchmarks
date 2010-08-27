;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: Jul 30, 2009

;; Finally I seem to have a program that doesn't cons at a very high
;; rate, like the previous one did.  It isn't terribly slow, either.

(ns revcomp
  (:gen-class))

(set! *warn-on-reflection* true)


(defn fasta-description-line
  "Return true when the line l is a FASTA description line"
  [l]
  (= \> (first (seq l))))


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
		    (int (cmap (char code)))
		    code))
		(range 256))))


(defn println-string-to-buffered-writer [#^java.io.BufferedWriter bw
					 #^java.lang.String s]
  (. bw write (.toCharArray s) 0 (count s))
  (. bw newLine))


(defn print-str-reverse-complement
  [#^java.io.BufferedWriter bw #^java.lang.String str
   to-print-before-nl max-line-len
   complement-fn]
  (let [max-line-len (int max-line-len)]
    (loop [i (int (dec (count str)))
	   to-print-before-nl (int to-print-before-nl)]
      (if (neg? i)
	;; Return how many characters are left to print before the
	;; next newline should be printed, so that the caller can pass
	;; that value into the next invocation of this function, if
	;; they wish.
	to-print-before-nl
	(let [next-to-print-before-nl (int (dec to-print-before-nl))]
	  ;; After changing complement-fn so it returns an int instead
	  ;; of a char, I thought it would be a good idea to then
	  ;; remove the first int call on the next line.  However,
	  ;; doing that makes it so Clojure has to use reflection to
	  ;; determine which Java write method needs to be called, and
	  ;; that slows things down much more than if the first call
	  ;; to int is there.
	  (. bw write (int (complement-fn (int (. str charAt i)))))
	  (when (zero? next-to-print-before-nl)
	    (. bw newLine))
	  (if (zero? next-to-print-before-nl)
	    (recur (dec i) max-line-len)
	    (recur (dec i) next-to-print-before-nl)))))))


(defn -main [& args]
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
      
      (loop [to-print-before-nl max-dna-chars-per-line
             str-seq (seq (reverse str-seq))]
        (if str-seq
          (let [next-to-print-before-nl
                (print-str-reverse-complement bw (first str-seq)
                                              to-print-before-nl
                                              max-dna-chars-per-line
                                              complement-dna-char-fn)]
            (recur next-to-print-before-nl (seq (next str-seq))))
          (when (not= to-print-before-nl max-dna-chars-per-line)
            (. bw newLine)))))
    (. bw flush))
  (. System (exit 0)))
