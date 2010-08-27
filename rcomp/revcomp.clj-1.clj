;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: Jul 29, 2009

(ns revcomp
  (:gen-class))

(set! *warn-on-reflection* true)

(defn fasta-description-line
  "Return true when the line l is a FASTA description line"
  [l]
  (= \> (first (seq l))))


(defn fasta-desc-dna-str-pairs
  "Take a sequence of lines from a FASTA format file.  Return a lazy sequence of 2-element vectors [desc dna-seq], where desc is a FASTA description line, and sdna-seq is the concatenated FASTA DNA sequence with that description.  If input file is big, you'll save lots of memory if you call this function in a with-open for the file, and don't hold on to the head of the lines parameter."
  [lines]
  (when-let [x (drop-while (fn [l]
                             (not (fasta-description-line l)))
                           lines)]
    (when-let [x (seq x)]
      (lazy-seq
	(let [[lines-before-next-desc next-desc-line-onwards]
	      (split-with (fn [l] (not (fasta-description-line l))) (rest x))]
	  (cons [(first x) (apply str lines-before-next-desc)]
		(fasta-desc-dna-str-pairs next-desc-line-onwards)))))))


;; If you do this, and invoke the macro many many times, it seems to
;; end up creating the same PersistentHashMap over and over again, on
;; each run-time call of the macro.  If you change it to a function
;; (using defn), as below, the run time is still the same.  Changing
;; it to a 'def', so the map is created only once, is much faster.

;;(defmacro complement-of-dna-char
;;  [c]
;;  `({\w \W, \W \W,
;;     \s \S, \S \S,
;;     \a \T, \A \T,
;;     \t \A, \T \A,
;;     \u \A, \U \A,
;;     \g \C, \G \C,
;;     \c \G, \C \G,
;;     \y \R, \Y \R,
;;     \r \Y, \R \Y,
;;     \k \M, \K \M,
;;     \m \K, \M \K,
;;     \b \V, \B \V,
;;     \d \H, \D \H,
;;     \h \D, \H \D,
;;     \v \B, \V \B,
;;     \n \N, \N \N } ~c))


;;(defn complement-of-dna-char
;;  [c]
;;  ({\w \W, \W \W,
;;    \s \S, \S \S,
;;    \a \T, \A \T,
;;    \t \A, \T \A,
;;    \u \A, \U \A,
;;    \g \C, \G \C,
;;    \c \G, \C \G,
;;    \y \R, \Y \R,
;;    \r \Y, \R \Y,
;;    \k \M, \K \M,
;;    \m \K, \M \K,
;;    \b \V, \B \V,
;;    \d \H, \D \H,
;;    \h \D, \H \D,
;;    \v \B, \V \B,
;;    \n \N, \N \N } c))


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


(defn reverse-complement-of-dna-seq [dna-seq-str]
  (let [comp complement-of-dna-char]
    (apply str (map (fn [c] (comp c))
		    (reverse (seq dna-seq-str))))))


(defn print-string-broken-into-lines [s max-len]
  (let [len (count s)]
    (loop [start 0]
      (let [next-start (+ start max-len)
	    end (min next-start len)]
	(println (subs s start end))
	(when (< next-start len)
	  (recur next-start))))))


(defn -main [& args]
  (let [max-dna-chars-per-line 60]
    (with-open [br (java.io.BufferedReader. *in*)]
      (doseq [[desc dna-seq] (fasta-desc-dna-str-pairs (line-seq br))]
        (println desc)
        (print-string-broken-into-lines (reverse-complement-of-dna-seq dna-seq)
                                        max-dna-chars-per-line))))
  (. System (exit 0)))
