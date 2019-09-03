;;   The Computer Language Benchmarks Game
;;   http://benchmarksgame.alioth.debian.org/
;;
;; contributed by Alex Miller, ported from Java version

(ns fannkuchredux
  (:require clojure.string)
  (:import [java.util.concurrent.atomic AtomicInteger])
  (:gen-class))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

(def ^:const NCHUNKS 150)
(def ^AtomicInteger task-id (AtomicInteger.))

(definterface Task
  (taskLoop [^long n])
  (runTask [^long task ^long n ^longs p ^longs pp ^longs counts])
  (firstPermutation [^long idx ^longs p ^longs pp ^longs counts])
  (^long countFlips [^longs p ^longs pp])
  (nextPermutation [^longs p ^longs counts]))

(deftype FannTask [^longs fact      ;; constant
                   ^long chunksz    ;; constant
                   ^long ntasks     ;; constant
                   ^longs max-flips ;; global scope
                   ^longs chk-sums  ;; global scope
                   ]
  Task
  (taskLoop [this n]
    (let [p (long-array n)
          pp (long-array n)
          counts (long-array n)]
      (loop []
        (let [task (.getAndIncrement task-id)]
          (when (< task ntasks)
            (.runTask this task n p pp counts)
            (recur))))))

  (runTask [this task n p pp counts]
    (let [^longs max-flips max-flips
          ^longs chk-sums chk-sums
          idx-min (* task chunksz)
          idx-max (min (aget fact n) (+ idx-min chunksz))]
      (.firstPermutation this idx-min p pp counts)
      (loop [mflips 1
             chksum 0
             i idx-min]
        (if (zero? (aget p 0))
          (let [new-mflips mflips
                new-chksum chksum
                new-i (inc i)]
            (if (< new-i idx-max)
              (do
                (.nextPermutation this p counts)
                (recur new-mflips new-chksum new-i))
              (do
                (aset max-flips task new-mflips)
                (aset chk-sums task new-chksum)
                nil)))
          (let [flips (.countFlips this p pp)
                new-mflips (max mflips flips)                      
                new-chksum (+ chksum (if (zero? (rem i 2)) flips (- flips)))
                new-i (inc i)]
            (if (< new-i idx-max)
              (do
                (.nextPermutation this p counts)
                (recur (long new-mflips) (long new-chksum) new-i))
              (do
                (aset max-flips task new-mflips)
                (aset chk-sums task new-chksum)
                nil)))))))
  
  (firstPermutation [_ idx p pp counts]
    (let [^longs fact fact
          pl (alength p)]
      (loop [i 0]
        (when (< i pl)
          (aset p i i)
          (recur (inc i))))
      (loop [i (dec (alength counts))
             idx idx]
        (when (> i 0)
          (let [fact-i (aget fact i)
                d (quot idx fact-i)]
            (aset counts i d)
            (System/arraycopy p 0 pp 0 (inc i))
            (loop [j 0]
              (if (<= j i)
                (let [jd (+ j d)
                      val (if (<= jd i)
                            (aget pp jd)
                            (aget pp (- jd i 1)))]
                  (aset p j val)
                  (recur (inc j)))))
            (recur (dec i) (long (rem idx fact-i))))))))
  
  (nextPermutation [_ p counts]    
    (let [f (aget p 1)]
      (aset p 1 (aget p 0))
      (aset p 0 f)
      (loop [i 1
             f f]
        (let [ci (inc (aget counts i))]
          (aset counts i ci)
          (when (> ci i)
            (aset counts i 0)
            (let [new-i (inc i)                
                  next (aget p 1)]
              (aset p 0 next)
              (loop [j 1]
                (when (< j new-i)
                  (let [j+1 (inc j)]
                    (aset p j (aget p j+1))
                    (recur j+1))))
              (aset p new-i f)
              (recur new-i next)))))))
  
  (countFlips [_ p pp]
    (let [flips 1
          f (aget p 0)]
      (if (zero? (aget p f))
        1
        (do
          (System/arraycopy p 0 pp 0 (alength pp))
          (loop [f f
                 flips flips]
            (let [new-flips (inc flips)]
              (loop [lo 1
                     hi (dec f)]
                (when (< lo hi)
                  (let [t (aget pp lo)]
                    (aset pp lo (aget pp hi))
                    (aset pp hi t)
                    (recur (inc lo) (dec hi)))))
              (let [t (aget pp f)]
                (aset pp f f)
                (if (zero? (aget pp t))
                  new-flips
                  (recur t new-flips))))))))))

(defn print-result [n res chk]
  (printf "%d\nPfannkuchen(%d) = %d\n" chk n res))

(defn fannkuch [^long n]
  (let [fact (long-array (concat [1] (reductions * (range 1 (inc n)))))
        chunksz (quot (+ (aget fact n) NCHUNKS -1) NCHUNKS)
        ntasks (quot (+ (aget fact n) chunksz -1) chunksz)
        max-flips (long-array ntasks)
        chk-sums (long-array ntasks)
        nthreads (.availableProcessors (Runtime/getRuntime))
        tasks (repeatedly nthreads #(->FannTask fact chunksz ntasks max-flips chk-sums))
        threads (into-array Thread (doall (map #(Thread. (fn [] (.taskLoop ^Task % n))) tasks)))]
    
    (doseq [^Thread t threads]
      (.start t))

    (doseq [^Thread t threads]
      (.join t))

    (print-result n (apply max max-flips) (reduce + chk-sums))))

(defn -main [& args]
  (let [n (if (seq args) (Long/parseLong (first args)) 12)]
    (cond (< n 0) (print-result n -1 -1)
          (> n 12) (print-result n -1 -1)
          (<= n 1) (print-result n 0 0)
          :else (fannkuch n)))
  (flush)
  (. System (exit 0)))
