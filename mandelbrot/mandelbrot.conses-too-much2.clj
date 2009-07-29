;; clj-run.sh does not send the program name as the first command line
;; argument -- only the command line arguments after the command name.

;; Needed for BufferedOutputStream
(import '(java.io BufferedOutputStream))

(def prog-name "mandelbrot")
(def args *command-line-args*)

(defn usage [exit-code]
  (println (format "usage: %s size [print-in-text-format]" prog-name))
  (println (format "    size must be a positive integer"))
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


;; Both of these versions of eight-bools-to-byte seem to time a bit
;; slower than bools-to-int

;; (defn eight-bools-to-byte
;;   [bools]
;;   (loop [b (byte 0)
;;          bools bools]
;;     (if-let [s (seq bools)]
;;       (recur (byte (+ (bit-shift-left b 1)
;;                       (if (first s) 1 0)))
;;              (rest s))
;;       b)))

;; (defn eight-bools-to-byte2
;;   [bools]
;;   (loop [b (byte 0)
;;          bools bools]
;;     (if-let [s (seq bools)]
;;       (if first
;; 	(recur (byte (inc (bit-shift-left b 1))) (rest s))
;; 	(recur (byte (bit-shift-left b 1)) (rest s)))
;;       b)))

;; bools-to-bytes and bools-to-bytes2 appear to be competitive in
;; performance, according to my quick timing tests:

;; (time (dotimes [n 1000] (bools-to-bytes (doall (map (fn [x] (= 0 (int (rand 2)))) (range 16000))))))
;; (time (dotimes [n 1000] (bools-to-bytes2 (doall (map (fn [x] (= 0 (int (rand 2)))) (range 16000))))))

(defn bools-to-bytes2
  [bools]
  (lazy-seq
    (when-let [s (seq bools)]
      (let [p (take 8 s)]
        (if (= 8 (count p))
          (cons (byte (bools-to-int p)) (bools-to-bytes2 (drop 8 s)))
          (list (byte (bools-to-int
                       (concat p (repeat (- 8 (count p)) false))))))))))


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
          (. ostream write (bytes-to-java-byte-arr r) 0 (count r)))
        (. ostream close)))
    (flush)))

(main size print-in-text-format)

(. System (exit 0))
