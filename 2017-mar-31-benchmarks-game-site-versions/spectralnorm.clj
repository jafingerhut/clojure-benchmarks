;; The Computer Language Benchmarks Game
;; http://benchmarksgame.alioth.debian.org/
;;
;; ported from Java #2
;; provided by Alex Miller

(ns spectralnorm
  (:import [java.util.concurrent CyclicBarrier]
           [clojure.lang Numbers])
  (:gen-class))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

(defmacro a [i j]
  `(/ 1.0 (double (+ (Numbers/unsignedShiftRightInt (* (+ ~i ~j) (+ ~i ~j 1)) 1) (inc ~i)))))

(defn mul-av [^doubles v ^doubles av ^long begin ^long end]
  (let [vl (alength v)]
    (loop [i begin
           j 0
           sum 0.0]
      (when (< i end)
        (if (< j vl)
          (recur i (inc j) (+ sum (* (a i j) (aget v j))))
          (do
            (aset av i sum)
            (recur (inc i) 0 0.0)))))))

(defn mul-atv [^doubles v ^doubles atv ^long begin ^long end]
  (let [vl (alength v)]
    (loop [i begin
           j 0
           sum 0.0]
      (when (< i end)
        (if (< j vl)
          (recur i (inc j) (+ sum (* (a j i) (aget v j))))
          (do
            (aset atv i sum)
            (recur (inc i) 0 0.0)))))))

(defn approximate [^doubles u ^doubles v ^doubles tmp
                   begin end ^CyclicBarrier barrier
                   t ^doubles mvbvs ^doubles mvvs]
  (let [begin (long begin)
        end (long end)
        t (int t)]
    (loop [i 0]
      (when (< i 10)
        (mul-av u tmp begin end)
        (.await barrier)
        (mul-atv tmp v begin end)
        (.await barrier)
        (mul-av v tmp begin end)
        (.await barrier)
        (mul-atv tmp u begin end)
        (.await barrier)
        (recur (inc i))))
    (loop [i begin
           mvbv 0.0
           mvv 0.0]
      (if (< i end)
        (let [vi (aget v i)]
          (recur (inc i)
                 (+ mvbv (* (aget u i) vi))
                 (+ mvv (* vi vi))))
        (do
          (aset mvbvs t mvbv)
          (aset mvvs t mvv))))))

(defn game [^long n]
  (let [u (double-array n)
        v (double-array n)
        tmp (double-array n)
        nthread (.availableProcessors (Runtime/getRuntime))
        nthread' (dec nthread)
        th (object-array nthread)
        mvbv (double-array nthread)
        mvv (double-array nthread)
        barrier (CyclicBarrier. nthread)
        chunk (quot n nthread)]
    (loop [i 0]
      (when (< i n)
        (aset u i 1.0)
        (recur (inc i))))
    (loop [i 0]
      (when (< i nthread)
        (let [r1 (* i chunk)
              r2 (long (if (< i nthread') (+ r1 chunk) n))
              thr (Thread. ^Runnable (fn [] (approximate u v tmp r1 r2 barrier i mvbv mvv)))]
          (aset th i thr)
          (.start thr)
          (recur (inc i)))))
    (loop [i 0
           vBv 0.0
           vv 0.0]
      (if (< i nthread)
        (let [t ^Thread (nth th i)]
          (.join t)
          (recur (inc i) (+ vBv (aget mvbv i)) (+ vv (aget mvv i))))
        (println (format "%.9f" (Math/sqrt (/ vBv vv))))))))

(defn -main [& args]
  (let [n (long (if (empty? args)
                  1000
                  (Long/parseLong ^String (first args))))]
    (game n)))
