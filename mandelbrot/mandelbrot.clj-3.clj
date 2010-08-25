;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: July, 2009

;; The function 'dot' is based on suggestions and improvements made by
;; these people posting to the Clojure Google group in April, 2009:

;; dmitri.sotnikov@gmail.com
;; William D. Lipe (atmcsld@gmail.com)
;; Paul Stadig (paul@stadig.name)
;; michael.messinides@invista.com
;; David Sletten
;; John Harrop


;; clj-run.sh sends the command line arguments, not including the
;; command name.

;; Needed for BufferedOutputStream
(import '(java.io BufferedOutputStream))

;;(set! *warn-on-reflection* true)

(def *default-modified-pmap-num-threads*
     (+ 2 (.. Runtime getRuntime availableProcessors)))

(defn usage [exit-code]
  (println (format "usage: %s size [num-threads [print-in-text-format]]"
                   *file*))
  (println (format "    size must be a positive integer"))
  (println (format "    num-threads is the maximum threads to use at once"))
  (println (format "        during the computation.  If 0 or not given, it"))
  (println (format "        defaults to the number of available cores plus 2,"))
  (println (format "        which is %d"
                   *default-modified-pmap-num-threads*))
  (. System (exit exit-code)))

(when (or (< (count *command-line-args*) 1) (> (count *command-line-args*) 3))
  (usage 1))
(when (not (re-matches #"^\d+$" (nth *command-line-args* 0)))
  (usage 1))
(def size (. Integer valueOf (nth *command-line-args* 0) 10))
(when (< size 1)
  (usage 1))
(def num-threads
     (if (>= (count *command-line-args*) 2)
       (do
         (when (not (re-matches #"^\d+$" (nth *command-line-args* 1)))
           (usage 1))
         (let [n (. Integer valueOf (nth *command-line-args* 1) 10)]
           (if (== n 0)
             *default-modified-pmap-num-threads*
             n)))
       *default-modified-pmap-num-threads*))
(def print-in-text-format (= (count *command-line-args*) 3))


(def max-iterations 50)
(def limit-square (double 4.0))

(defn dot [r i]
  (let [f2 (double 2.0)
        limit-square (double limit-square)
        iterations-remaining (int max-iterations)
        pr (double r)
        pi (double i)]
    ;; The loop below is similar to the one in the Perl subroutine dot
    ;; in mandelbrot.perl, with these name correspondences:
    ;; pr <-> Cr, pi <-> Ci, zi <-> Zi, zr <-> Zr, zr2 <-> Tr, zi2 <-> Ti
    (loop [zr (double 0.0)
           zi (double 0.0)
           i (int (inc iterations-remaining))]
      (let [zr2 (* zr zr)
            zi2 (* zi zi)]
        (if (and (not (zero? i))
                 (< (+ zr2 zi2) limit-square))
          (recur (+ (- zr2 zi2) pr)
                 (+ (* (* f2 zr) zi) pi)
                 (unchecked-dec i))
          (zero? i))))))


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
      ;; then
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
      ;; else
      (if (= num-filled-bits 0)
	result
	(conj result (byte (bit-shift-left b (- 8 num-filled-bits))))))))


(defn my-lazy-map
  [f coll]
  (lazy-seq
    (when-let [s (seq coll)]
      (cons (f (first s)) (my-lazy-map f (rest s))))))


(defn modified-pmap
  "Like pmap from Clojure 1.1, but with only as much parallelism as
  there are available processors.  Uses my-lazy-map instead of map
  from core.clj, since that version of map can use unwanted additional
  parallelism for chunked collections, like ranges."
  ([num-threads f coll]
     (if (== num-threads 1)
       (map f coll)
       (let [n (if (>= num-threads 2) (dec num-threads) 1)
             rets (my-lazy-map #(future (f %)) coll)
             step (fn step [[x & xs :as vs] fs]
                    (lazy-seq
                      (if-let [s (seq fs)]
                        (cons (deref x) (step xs (rest s)))
                        (map deref vs))))]
         (step rets (drop n rets)))))
  ([num-threads f coll & colls]
     (let [step (fn step [cs]
                  (lazy-seq
                    (let [ss (my-lazy-map seq cs)]
                      (when (every? identity ss)
                        (cons (my-lazy-map first ss) (step (my-lazy-map rest ss)))))))]
       (modified-pmap num-threads #(apply f %) (step (cons coll colls))))))
  

;;(defn noisy-compute-row [x-vals y-val-ind two-over-size y-offset]
;;  (println (str "noisy-compute-row begin " y-val-ind))
;;  (let [ret-val (compute-row x-vals
;;                             (index-to-val y-val-ind two-over-size y-offset))]
;;    (println (str "noisy-compute-row end " y-val-ind))
;;    ret-val))


(defn compute-rows [size num-threads]
  (let [two-over-size (double (/ 2.0 size))
        x-offset (double -1.5)
        y-offset (double -1.0)
        x-vals (map #(index-to-val % two-over-size x-offset) (range size))]
    (modified-pmap num-threads
                   #(compute-row x-vals
                                 (index-to-val % two-over-size y-offset))
;;                   #(noisy-compute-row x-vals % two-over-size y-offset)
                   (range size))))


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
