(ns com.andy_fingerhut.memuse)
;;(ns clojure.memuse)

;;(set! *warn-on-reflection* true)

(def n (. Integer valueOf (nth *command-line-args* 0)))
(def m (. Integer valueOf (nth *command-line-args* 1)))
(def l (. Integer valueOf (nth *command-line-args* 2)))

(println (str "n=" n "  m=" m "  l=" l))

(def r (java.util.Random.))
(. r setSeed 1234567890)
(defn rands [] (repeatedly #(.nextGaussian #^java.util.Random r)))

(defn random-double-array [n]
  (let [n (int n)
        #^doubles a (double-array n)]
    (loop [i (int 0)]
      (if (< i n)
        (do
          (aset a i (.nextGaussian #^java.util.Random r))
          (recur (inc i)))
        a))))

(defn random-long-array [n]
  (let [n (int n)
        #^longs a (long-array n)]
    (loop [i (int 0)]
      (if (< i n)
        (do
          (aset a i (.nextLong #^java.util.Random r))
          (recur (inc i)))
        a))))

(defstruct twod-point :x :y)

(def list-of-stuff (doall
                    (condp = m
                      0 '(a)
                      1 (take n (rands))
                      2 (take n (partition 2 (rands)))
                      3 (vec (take n (rands)))
                      4 (vec (take n (partition 2 (rands))))
                      5 (vec (take n (map vec (partition 2 (rands)))))
                      6 (double-array n)
                      7 (take n (repeatedly (fn [] (double-array 2))))
                      8 (int-array n)
                      9 (take n (repeatedly (fn [] (int-array 2))))
                      10 (long-array n)
                      11 (take n (repeatedly (fn [] (long-array 2))))
                      12 (random-double-array n)
                      13 (take n (repeatedly (fn [] (random-double-array 2))))
                      14 (random-long-array n)
                      15 (take n (repeatedly (fn [] (random-long-array 2))))
                      16 (vec (take n (map (fn [[x y]] (struct twod-point x y))
                                           (partition 2 (rands)))))
                      )))

;; Stick around, so we can attach things to it like jmap

(println "Done.  Going to sleep now.  Good night.")
(flush)
;; Make a let binding of this to make it easier to find in jswat.  I
;; don't know if jswat can view def's or not, but I haven't found them
;; yet.
(let [list-of-stuff list-of-stuff]
  (condp = l
    0 (loop [i (int 0)]
        (when (zero? (rem i 100000000))
          (println (str "i=" i)))
        (recur (inc i)))
    1 (loop [i (int 0)]
        (Thread/sleep 1000)
        (recur (inc i)))
    ))
