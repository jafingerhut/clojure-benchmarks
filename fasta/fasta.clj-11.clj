;;   The Computer Language Benchmarks Game
;;   http://shootout.alioth.debian.org/

;; contributed by Bill James
;; speed improvements by Andy Fingerhut

(ns fasta
  (:gen-class))

(set! *warn-on-reflection* true)


(def *width* 60)
(def *lookup-size* 222000)


(def *alu* (str "GGCCGGGCGCGGTGGCTCACGCCTGTAATCCCAGCACTTTGG"
                "GAGGCCGAGGCGGGCGGATCACCTGAGGTCAGGAGTTCGAGA"
                "CCAGCCTGGCCAACATGGTGAAACCCCGTCTCTACTAAAAAT"
                "ACAAAAATTAGCCGGGCGTGGTGGCGCGCGCCTGTAATCCCA"
                "GCTACTCGGGAGGCTGAGGCAGGAGAATCGCTTGAACCCGGG"
                "AGGCGGAGGTTGCAGTGAGCCGAGATCGCGCCACTGCACTCC"
                "AGCCTGGGCGACAGAGCGAGACTCCGTCTCAAAAA"))

(def *codes* "acgtBDHKMNRSVWY")

(def *iub* [0.27 0.12 0.12 0.27 0.02 0.02 0.02 0.02
           0.02 0.02 0.02 0.02 0.02 0.02 0.02])

(def *homosapiens* [0.3029549426680 0.1979883004921
                   0.1975473066391 0.3015094502008])


(defn find-index [f coll]
  (loop [i (int 0)
         s (seq coll)]
    (if (f (first s))
      i
      (recur (unchecked-inc i) (rest s)))))


(def random-seed (int-array [42]))
(let [scale (double (/ *lookup-size* 139968))]
  (defn gen-random-fast []
    (let [^ints random-seed random-seed
          IM (int 139968)
          IA (int 3877)
          IC (int 29573)
          zero (int 0)
          new-seed (int (unchecked-remainder
                         (unchecked-add
                          (unchecked-multiply
                           (aget random-seed zero) IA) IC) IM))
          ;; I had the (aset random-seed zero new-seed) in the body of
          ;; the let before, but strangely the Clojure compiler
          ;; generated an unnecessary call to Integer.valueOf() to
          ;; convert aset's return value to an Integer and then
          ;; discard that value.  By assigning the return value of
          ;; aset to throwaway-val, the compiler actually generates
          ;; faster code.
          throwaway-val (int (aset random-seed zero new-seed))]
      (int (* new-seed scale)))))


;; Takes a vector of probabilities.
(defn make-cumulative [v]
  (vec (map #(reduce + (subvec v 0 %)) (range 1 (inc (count v))))))


;; Takes a vector of cumulative probabilities.
(defn make-lookup-table [v]
  (let [sz (int *lookup-size*)
        lookup-scale (- sz 0.0001)
        ^ints a (int-array sz)]
    (dotimes [i sz]
      (aset a i (int (find-index #(<= (/ i lookup-scale) %) v))))
    a))


(defn cycle-bytes [source source-size n
                   ^java.io.BufferedOutputStream ostream]
  (let [source-size (int source-size)
        width (int *width*)
        width+1 (int (inc width))
        buffer-size (int (* width+1 4096))
        buffer (byte-array buffer-size (byte 10))]
    (loop [i (int 0)
           j (int 0)
           n (int n)]
      (System/arraycopy source i buffer j width)
      (if (> n width)
        (recur (int (unchecked-remainder
                     (unchecked-add i width) source-size))
               (int (let [j (unchecked-add j width+1)]
                      (if (== j buffer-size)
                        (do (.write ostream buffer) (int 0))
                        j)))
               (unchecked-subtract n width))
        (do
          (aset buffer (+ j n) (byte 10))
          (.write ostream buffer (int 0) (+ j n 1)))))))


(defn fasta-repeat [n ^java.io.BufferedOutputStream ostream]
  (let [source (.getBytes (str *alu* *alu*))]
    (cycle-bytes source (count *alu*) n ostream)))


(defn fasta-random [probs n ^java.io.BufferedOutputStream ostream]
  (let [codes (.getBytes (str *codes*))
        lookup-table (ints (make-lookup-table
                            (make-cumulative probs)))
        width (int *width*)
        width-1 (int (dec width))
        num-lines-in-buffer (int 2000)
        buf-size (int (* num-lines-in-buffer (inc width)))
        buffer (byte-array buf-size)]
    (loop [n (int n)      ;; The number of characters left to generate.
           j (int width)  ;; The number of characters left to
                          ;; generate on the current line of output
                          ;; before printing a newline.
           i (int 0)]     ;; Number of characters put in buffer so far.
      (if (zero? n)
        (do
          (.write ostream buffer (int 0) i)
          (when (not= (aget buffer (dec i)) (byte 10))
            (aset buffer (int 0) (byte 10))
            (.write ostream buffer (int 0) (int 1))))
        ;; else
        (let [rand-byte (aget codes (aget lookup-table (gen-random-fast)))]
          (if (== i buf-size)
            (do
              ;; buffer is full.  write it and start over.
              (.write ostream buffer (int 0) buf-size)
              (aset buffer (int 0) rand-byte)
              (recur (unchecked-dec n) width-1 (int 1)))
            (do
              (aset buffer i rand-byte)
              (if (== j (int 1))
                ;; then
                (do
                  (aset buffer (unchecked-inc i) (byte 10)) ;; add newline
                  (recur (unchecked-dec n) width (unchecked-add i 2)))
                ;; else
                (recur (unchecked-dec n) (unchecked-dec j)
                       (unchecked-inc i))))))))))


(defn write-line [s ^java.io.BufferedOutputStream stream]
  (.write stream (.getBytes (str s "\n"))))


(defn -main [& args]
  (let [n (Integer/parseInt (nth args 0))
        ostream (java.io.BufferedOutputStream. System/out)
        start-time (System/currentTimeMillis)]
    (write-line ">ONE Homo sapiens alu" ostream)
    (fasta-repeat (* n 2) ostream)
    (write-line ">TWO IUB ambiguity codes" ostream)
    (fasta-random *iub* (* n 3) ostream)
    (write-line ">THREE Homo sapiens frequency" ostream)
    (fasta-random *homosapiens* (* n 5) ostream)
    (.flush ostream)))
