;;   The Computer Language Benchmarks Game
;;   http://benchmarksgame.alioth.debian.org/
;;
;; ported from Scala revcomp #8
;; contributed by Alex Miller

(ns revcomp
  (:gen-class)
  (:import [java.util.concurrent ForkJoinPool ForkJoinTask]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

(def bs (let [bs (byte-array 128)
              a "ACBDGHK\nMNSRUTWVYacbdghkmnsrutwvy"
              b "TGVHCDM\nKNSYAAWBRTGVHCDMKNSYAAWBR"
              c (.length a)]
          (loop [i 0]
            (when (< i c)
              (aset bs (.charAt a i) (byte (int (.charAt b i))))
              (recur (inc i))))
          bs))

(defn inner ^long [^long i ^long len ^bytes buf]
  (let [bs ^bytes bs]
    (if (< i len)
      (let [b (int (aget buf i))]
        (if (= b (int 62))  ;; >
          (inc i)
          (do
            (aset buf i (aget bs b))
            (recur (inc i) len buf))))
      i)))

(defn reverse-task [^bytes buf ^long begin ^long end]
  (fn []
    (let [buf ^bytes buf]
      (loop [begin begin
             end end]
        (when (< begin end)
          (let [bb (long (aget buf begin))]
            (if (= bb 10)
              (recur (inc begin) end)
              (let [be (long (aget buf end))]
                (if (= be 10)
                  (recur begin (dec end))
                  (do
                    (aset buf begin (byte be))
                    (aset buf end (byte bb))
                    (recur (inc begin) (dec end))))))))))))

(defn -main [& args]
  (let [pool (ForkJoinPool/commonPool)
        len (long (.available System/in))
        buf (byte-array len)]
    (.read System/in buf)
    (loop [i 0
           tasks []]
      (if (< i len)
        (let [b (long (aget buf i))]
          (if (= b 10)
            (let [j (inner i len buf)
                  end (- j 2)
                  task (ForkJoinTask/adapt (reverse-task buf i end) end)]
              (.execute pool task)
              (recur (inc j) (conj tasks task)))
            (recur (inc i) tasks)))
        (loop [i 0
               last 0]
          (if (< i (count tasks))
            (let [t ^ForkJoinTask (nth tasks i)
                  r (long (.join t))]
              (.write System/out buf last (- r last))
              (recur (inc i) r))
            (.write System/out buf last (- len last))))))))
