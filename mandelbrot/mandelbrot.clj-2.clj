;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: July, 2009

;; The function 'dot' is based on suggestions and improvements made by
;; these people posting to the Clojure Google group in April, 2009:

;; dmitri.sotnikov@gmail.com
;; William D. Lipe (atmcsld@gmail.com)
;; Paul Stadig (paul@stadig.name)
;; michael.messinides@invista.com
;; David Sletten


;; clj-run.sh sends the command line arguments, not including the
;; command name.

;; Needed for BufferedOutputStream
(import '(java.io BufferedOutputStream))

;;(set! *warn-on-reflection* true)

(def prog-name "mandelbrot")
(def args *command-line-args*)

(defn usage [exit-code]
  (println (format "usage: %s size [num-threads [print-in-text-format]]"
                   prog-name))
  (println (format "    size and num-threads must be positive integers"))
  (println (format "    num-threads defaults to the number of available processors if not specified"))
  (. System (exit exit-code)))

(when (or (< (count args) 1) (> (count args) 3))
  (usage 1))
(when (not (re-matches #"^\d+$" (nth args 0)))
  (usage 1))
(def size (. Integer valueOf (nth args 0) 10))
(when (< size 1)
  (usage 1))
(def num-threads
     (when (>= (count args) 2)
       (when (not (re-matches #"^\d+$" (nth args 1)))
         (usage 1))
       (let [n (. Integer valueOf (nth args 1) 10)]
         (when (< n 1)
           (usage 1))
         n)))
(def print-in-text-format (= (count args) 3))


(def max-iterations 50)
(def limit-square (double 4.0))

(defn dot [r i]
  (let [f2 (double 2.0)
        limit-square limit-square
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


(defn index-to-val [i scale-fac offset]
  (+ (* i scale-fac) offset))


;; I had a much more sequence-y implementation of this before, but it
;; allocated garbage very quickly, which caused the program to slow
;; down dramatically once it hit the heap limit and start garbage
;; collecting frequently.

(defn compute-row
  [x-vals y]
  (loop [b (int 0)
	 num-filled-bits (int 0)
	 result []
	 x-vals x-vals]
    (if-let [s (seq x-vals)]
      ; then
      (let [new-bit (int (if (dot (first s) y) 1 0))
	    new-b (int (+ (bit-shift-left b 1) new-bit))]
	(if (= num-filled-bits 7)
	  (recur (int 0)
		 (int 0)
		 (conj result (byte new-b))
		 (rest s))
	  (recur new-b
		 (int (inc num-filled-bits))
		 result
		 (rest s))))
      ; else
      (if (= num-filled-bits 0)
	result
	(conj result (byte (bit-shift-left b (- 8 num-filled-bits))))))))


(defn modified-pmap
  "Like pmap from Clojure 1.1, but with only as much parallelism as
  there are available processors."
  [f coll & n]
  (let [n (or (first n) (.. Runtime getRuntime availableProcessors))
	rets (map #(future (f %)) coll)
	step (fn step [[x & xs :as vs] fs]
	       (lazy-seq
		(if-let [s (seq fs)]
		  (cons (deref x) (step xs (rest s)))
		  (map deref vs))))]
    (step rets (drop n rets))))


(defn compute-rows [size & nthreads]
  (let [two-over-size (double (/ 2.0 size))
        x-offset (double -1.5)
        y-offset (double -1.0)
        x-vals (map #(index-to-val % two-over-size x-offset) (range size))]
    (for [y (modified-pmap #(index-to-val % two-over-size y-offset)
                           (range size)
                           (first nthreads))]
      (compute-row x-vals y))))


(defn main [size num-threads print-in-text-format]
  (let [rows (compute-rows size num-threads)]
    (println "P4")
    (println (format "%d %d" size size))
    (if print-in-text-format
      (doseq [r rows]
        (doseq [byte r]
          (print (format " %02x" byte)))
        (newline))
      ;; else print in default PBM format
      (let [ostream (BufferedOutputStream. System/out)]
        (doseq [r rows]
          (. ostream write (into-array Byte/TYPE r) 0 (count r)))
        (. ostream close)))
    (flush)))


(main size num-threads print-in-text-format)

(. System (exit 0))
