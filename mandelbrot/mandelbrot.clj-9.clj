;;   The Computer Language Benchmarks Game
;;   http://shootout.alioth.debian.org/

;; contributed by Andy Fingerhut

;; The function 'dot' is based on suggestions and improvements made by
;; these people posting to the Clojure Google group in April, 2009:
;; dmitri.sotnikov, William D. Lipe, Paul Stadig, michael.messinides
;; David Sletten, John Harrop

;; change by Marko Kocic
;; reduced code size by removing functions already present in Clojure

;; change by Andy Fingerhut
;; Use Java primitive double arrays instead of Clojure sequences in a
;; few inner loops.  This is a sequential version using map instead of
;; pmap, since sequential code tends to use less CPU time on 1-core
;; machines.

(ns mandelbrot
  (:gen-class)
  (:import (java.io BufferedOutputStream)))

(set! *warn-on-reflection* true)


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
           i (int (unchecked-inc iterations-remaining))]
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


(defn ubyte [val]
  (if (>= val 128)
    (byte (- val 256))
    (byte val)))


(defn compute-row [#^doubles x-vals y]
  (let [y (double y)
        n (int (alength x-vals))
        num-bytes-out (int (unchecked-divide (unchecked-add n 7) 8))
        #^bytes result (byte-array num-bytes-out)
        zero (int 0)
        one (int 1)]
    (loop [i (int zero)
           b (int zero)
           num-filled-bits zero
           result-idx (int zero)]
      (if (== i n)
        (do
          (when (not (zero? num-filled-bits))
            (let [last-byte-val
                  (byte (ubyte (bit-shift-left b (- 8 num-filled-bits))))]
            (aset result result-idx last-byte-val)))
          result)
        ;; else
        (let [new-bit (int (if (dot (aget x-vals i) y) one zero))
              new-b (int (unchecked-add (bit-shift-left b one) new-bit))]
          (if (== num-filled-bits 7)
            (let [byte-val (byte (ubyte new-b))]
              (aset result result-idx byte-val)
              (recur (unchecked-inc i)
                     zero
                     zero
                     (unchecked-inc result-idx)))
            (recur (unchecked-inc i)
                   new-b
                   (unchecked-inc num-filled-bits)
                   result-idx)))))))


(defn compute-rows [size]
  (let [two-over-size (double (/ 2.0 size))
        x-offset (double -1.5)
        y-offset (double -1.0)
        x-vals (double-array size (map #(index-to-val % two-over-size x-offset)
                                       (range size)))
        y-vals (double-array size (map #(index-to-val % two-over-size y-offset)
                                       (range size)))]
    (map #(compute-row x-vals (aget y-vals %))
         (range size))))


(defn do-mandelbrot [size]
  (let [rows (compute-rows size)]
    (printf "P4\n")
    (printf "%d %d\n" size size)
    (flush)
    (let [ostream (BufferedOutputStream. System/out)]
      (doseq [r rows]
        (. ostream write r 0 (count r)))
      (. ostream close))
    (flush)))


(defn -main [& args]
  (let [size (. Integer valueOf (nth args 0) 10)]
    (do-mandelbrot size)
    (shutdown-agents)))
