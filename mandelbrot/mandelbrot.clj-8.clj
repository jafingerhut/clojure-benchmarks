;;   The Computer Language Benchmarks Game
;;   http://shootout.alioth.debian.org/

;; contributed by Andy Fingerhut

;; The function 'dot' is based on suggestions and improvements made by
;; these people posting to the Clojure Google group in April, 2009:
;; dmitri.sotnikov, William D. Lipe, Paul Stadig, michael.messinides
;; David Sletten, John Harrop

;; change by Marko Kocic
;; reduced code size by removing functions already present in Clojure

(ns mandelbrot
  (:gen-class)
  (:import (java.io BufferedOutputStream)))

(set! *warn-on-reflection* true)

;; Handle slight difference in function name between Clojure 1.2.0 and
;; 1.3.0-alpha1.
(defmacro my-unchecked-dec-int [& args]
  (if (and (== (*clojure-version* :major) 1)
           (== (*clojure-version* :minor) 2))
    `(unchecked-dec ~@args)
    `(unchecked-dec-int ~@args)))

(defmacro my-unchecked-inc-int [& args]
  (if (and (== (*clojure-version* :major) 1)
           (== (*clojure-version* :minor) 2))
    `(unchecked-inc ~@args)
    `(unchecked-inc-int ~@args)))


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
           i (int (my-unchecked-inc-int iterations-remaining))]
      (let [zr2 (* zr zr)
            zi2 (* zi zi)]
        (if (and (not (zero? i))
                 (< (+ zr2 zi2) limit-square))
          (recur (+ (- zr2 zi2) pr)
                 (+ (* (* f2 zr) zi) pi)
                 (my-unchecked-dec-int i))
          (zero? i))))))


(defn index-to-val [i scale-fac offset]
  (+ (* i scale-fac) offset))


(defn ubyte [val]
  (if (>= val 128)
    (byte (- val 256))
    (byte val)))


(defn compute-row [x-vals y]
    (loop [b (int 0)
           num-filled-bits (int 0)
           result (transient [])
           x-vals x-vals]
      (if-let [s (seq x-vals)]
        ;; then
        (let [new-bit (int (if (dot (first s) y) 1 0))
              new-b (int (+ (bit-shift-left b 1) new-bit))]
          (if (== num-filled-bits 7)
            (recur (int 0)
                   (int 0)
                   (conj! result (ubyte new-b))
                   (rest s))
            (recur new-b
                   (int (inc num-filled-bits))
                   result
                   (rest s))))
        ;; else
        (if (zero? num-filled-bits)
          (persistent! result)
          (persistent! (conj! result
                              (ubyte (bit-shift-left b (- 8 num-filled-bits)))))
          ))))


(defn compute-rows [size]
  (let [two-over-size (double (/ 2.0 size))
        x-offset (double -1.5)
        y-offset (double -1.0)
        x-vals (map #(index-to-val % two-over-size x-offset) (range size))]
    (pmap #(compute-row x-vals
                        (index-to-val % two-over-size y-offset))
          (range size))))


(defn do-mandelbrot [size]
  (let [rows (compute-rows size)]
    (printf "P4\n")
    (printf "%d %d\n" size size)
    (flush)
    (let [ostream (BufferedOutputStream. System/out)]
      (doseq [r rows]
        (. ostream write (into-array Byte/TYPE r) 0 (count r)))
      (. ostream close))
    (flush)))


(defn usage [exit-code]
  (println (format "usage: %s size" *file*))
  (println (format "    size must be a positive integer"))
  (. System (exit exit-code)))


(defn -main [& args]
  (when (not= (count args) 1)
    (usage 1))
  (when (not (re-matches #"^\d+$" (nth args 0)))
    (usage 1))
  (def size (. Integer valueOf (nth args 0) 10))
  (when (< size 1)
    (usage 1))
  (do-mandelbrot size)
  (shutdown-agents))
