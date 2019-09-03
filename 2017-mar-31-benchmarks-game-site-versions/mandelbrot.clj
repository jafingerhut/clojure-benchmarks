;;   The Computer Language Benchmarks Game
;;   http://benchmarksgame.alioth.debian.org/
;;
;; ported from Java #2
;; contributed by Alex Miller

(ns mandelbrot
  (:gen-class)
  (:import (java.io BufferedOutputStream)
           (java.util.concurrent.atomic AtomicInteger)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

(def yct (AtomicInteger.))
(def crb nil)
(def cib nil)
(def out nil)

(defn jloop ^long [^longs xy ^doubles state ^long res ^long i]
  (let [x (aget xy 0)
        y (aget xy 1)
        crb ^doubles crb
        cib ^doubles cib]
    (loop [b 0
           j 49]
      (if (> j 0)
        (let [zr1 (aget state 0)
              zi1 (aget state 1)
              nzr1 (+ (- (* zr1 zr1) (* zi1 zi1)) (aget crb (+ x i)))
              nzi1 (+ (* zr1 zi1) (* zr1 zi1) (aget cib y))
              zr2 (aget state 2)
              zi2 (aget state 3)              
              nzr2 (+ (- (* zr2 zr2) (* zi2 zi2)) (aget crb (+ x i 1)))
              nzi2 (+ (* zr2 zi2) (* zr2 zi2) (aget cib y))]
          (aset state 0 nzr1)
          (aset state 1 nzi1)
          (aset state 2 nzr2)
          (aset state 3 nzi2)
          (if (> (+ (* nzr1 nzr1) (* nzi1 nzi1)) 4)
            (let [newb (bit-or b 2)]
              (if (= newb 3)
                (recur newb 0)
                (if (> (+ (* nzr2 nzr2) (* nzi2 nzi2)) 4)
                  (recur 3 0)
                  (recur newb (dec j)))))
            (if (> (+ (* nzr2 nzr2) (* nzi2 nzi2)) 4)
              (let [newb (bit-or b 1)]
                (if (= newb 3)
                  (recur newb 0)
                  (recur newb (dec j))))
              (recur b (dec j)))))
        (+ (bit-shift-left res 2) b)))))

(defn get-byte ^long [^long x ^long y]
  (let [crb ^doubles crb
        cib ^doubles cib
        out ^objects out
        xy (long-array 2)
        state (double-array 4)] ;; [zr1 zi1 zr2 zi2]
    (aset xy 0 x)
    (aset xy 1 y)
    (loop [res 0
           i 0]
      (if (< i 8)
        (do
          (aset state 0 (aget crb (+ x i)))
          (aset state 1 (aget cib y))
          (aset state 2 (aget crb (+ x i 1)))
          (aset state 3 (aget cib y))
          (recur (jloop xy state res i) (+ i 2)))
        (bit-xor res -1)))))

(defn put-line [^long y ^bytes line]
  (let [linelen (alength line)]
    (loop [xb 0]
      (when (< xb linelen)
        (aset line xb (byte (get-byte (* xb 8) y)))
        (recur (inc xb))))))

(defn putter [out]
  (let [yct ^AtomicInteger yct
        out ^objects out
        outlen (alength out)]
    (fn []
      (loop [y (.getAndIncrement yct)]
        (when (< y outlen)
          (do
            (put-line y (aget out y))
            (recur (.getAndIncrement yct))))))))

(defn -main [& args]
  (let [n (if (pos? (count args)) (. Long parseLong (nth args 0) 10) 6000)
        np7 (+ n 7)
        invn (/ 2.0 n)
        threads (* 2 (.availableProcessors (Runtime/getRuntime)))
        crb ^doubles (double-array np7)
        cib ^doubles (double-array np7)
        out ^objects (into-array (repeatedly n #(byte-array (quot (+ n 7) 8))))]
    (alter-var-root #'crb (constantly crb))
    (alter-var-root #'cib (constantly cib))
    (alter-var-root #'out (constantly out))
    (loop [i 0]
      (when (< i n)
        (let [m (* invn i)]        
          (aset cib i (- m 1.0))
          (aset crb i (- m 1.5))
          (recur (inc i)))))
    (loop [i 0
           pool []]
      (if (< i threads)
        (recur (inc i) (conj pool (Thread. ^Runnable (putter out))))
        (do
          (doseq [^Thread t pool] (.start t))
          (let [stream (BufferedOutputStream. System/out)]
            (.write stream ^bytes (.getBytes (format "P4\n%d %d\n" n n)))
            (doseq [^Thread t pool] (.join t))
            (loop [i 0]
              (if (< i n)
                (do
                  (.write stream ^bytes (aget out i))
                  (recur (inc i)))
                (.close stream)))))))))
