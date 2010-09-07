;; clj-run.sh does not send the program name as the first command line
;; argument -- only the command line arguments after the command name.

;; Needed for BufferedOutputStream
(import '(java.io BufferedOutputStream))

(def prog-name "mandelbrot")
(def args *command-line-args*)

(defn usage [exit-code]
  (printf "usage: %s size [print-in-text-format]\n" prog-name)
  (printf "    size must be a positive integer\n")
  (flush)
  (. System (exit exit-code)))

(when (or (< (count args) 1) (> (count args) 2))
  (usage 1))
(when (not (re-matches #"^\d+$" (nth args 0)))
  (usage 1))
(def size (. Integer valueOf (nth args 0) 10))
(when (< size 1)
  (usage 1))
(def print-in-text-format (= (count args) 2))


(def max-iterations 50)
(def limit-square (double 4.0))

(defn dot [r i]
  (let [f2 (double 2.0)
	limit-square limit-square
	;; iterations-remaining (dec max-iterations)
	iterations-remaining max-iterations
	pr (double r)
	pi (double i)]
    ;; The loop below is similar to the one in the Perl subroutine dot
    ;; in mandelbrot.perl, with these name correspondences:
    ;; pr <-> Cr, pi <-> Ci, zi <-> Zi, zr <-> Zr, zr2 <-> Tr, zi2 <-> Ti
    (loop [zr (double 0.0)
	   zi (double 0.0)
	   zr2 (double 0.0)
	   zi2 (double 0.0)
	   iterations-remaining iterations-remaining]
      (if (and (not (neg? iterations-remaining))
	       (< (+ zr2 zi2) limit-square))
	(let [new-zi (double (+ (* (* f2 zr) zi) pi))
	      new-zr (double (+ (- zr2 zi2) pr))
	      new-zr2 (double (* new-zr new-zr))
	      new-zi2 (double (* new-zi new-zi))]
	  (recur new-zr new-zi new-zr2 new-zi2 (dec iterations-remaining)))
	(neg? iterations-remaining)))))


;; (defn round-up-to-next-multiple-of [n mult]
;;   (int (/ (+ n (dec mult)) mult)))

;; (def num-line-bytes (round-up-to-next-multiple-of size 8))
;; (def one-line-bytes (make-array (. Byte TYPE) num-line-bytes))


(defn compute-row-bool [x-vals y]
  (map #(dot % y) x-vals))


(defn bools-to-int
  "Convert a sequence of bools to an integer.  The first bool determines the most significant bit of the integer (false becomes 0, true becomes 1).  The second bool determines the next most significant bit of the integer, and so on."
  [bools]
  (reduce (fn [n b]
	    (if b
	      (inc (bit-shift-left n 1))
	      (bit-shift-left n 1)))
	  0 bools))


(defn partition-with-padding
  "Returns a lazy sequence of lists of n items each, at offsets n
  apart.  If coll does not contain a multiple of n items, it is padded
  with enough copies of the item i at the end so that it does."
  [n coll i]
  (lazy-seq
    (when-let [s (seq coll)]
      (let [p (take n s)]
        (if (= n (count p))
          (cons p (partition-with-padding n (drop n s) i))
	  ;; otherwise, pad the last partition with copies of i until
	  ;; it is length n
	  (list (concat p (repeat (- n (count p)) i))))))))


(defn bools-to-bytes [bools]
  (map #(byte (bools-to-int %)) (partition-with-padding 8 bools false)))


;; It seems that (aset byte-arr i 10) fails for an array containing
;; Java primitive type Byte.  The documentation for aset says that it
;; works on Java arrays of reference types.  Why not also arrays of
;; primitive types?

;; (defn bytes-to-java-byte-arr [bytes]
;;   (let [n (count bytes)
;; 	byte-arr (make-array (. Byte TYPE) n)]
;;     (loop [i 0
;; 	   bytes bytes]
;;       (when-let [s (seq bytes)]
;; 	(aset byte-arr i (first s))
;; 	(recur (inc i) (rest s))))
;;     byte-arr))

(defn bytes-to-java-byte-arr [bytes]
  (into-array Byte/TYPE bytes))


(defn index-to-val [i scale-fac offset]
  (+ (* i scale-fac) offset))


(defn rows-calculated-sequentially [size]
  (let [two-over-size (double (/ 2.0 size))
	x-offset (double -1.5)
	y-offset (double -1.0)
	x-vals (map #(index-to-val % two-over-size x-offset) (range size))]
    (for [y (map #(index-to-val % two-over-size y-offset) (range size))]
      (bools-to-bytes (compute-row-bool x-vals y)))))


(defn main [size print-in-text-format]
  (let [rows (rows-calculated-sequentially size)]
    (printf "P4\n")
    (printf "%d %d\n" size size)
    (if print-in-text-format
      (doseq [r rows]
	(doseq [byte r]
          (printf " %02x" byte))
	(newline))
      ;; else print in default PBM format
      (let [ostream (BufferedOutputStream. System/out)]
	(doseq [r rows]
	  (. ostream write (bytes-to-java-byte-arr r) 0 (count r)))
	(. ostream close)))
    (flush)))

(main size print-in-text-format)

(. System (exit 0))
