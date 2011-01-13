(ns memuse
;  (:use fingertree)
  (:gen-class))

;;(set! *warn-on-reflection* true)

(def r (java.util.Random.))
(. ^java.util.Random r setSeed 1234567890)
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

(defn random-boxed-double-array [n]
  (let [n (int n)
        a (object-array n)]
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


;(defrecord Len-Meter [^int len])
;(def measure-len (constantly (Len-Meter. 1)))
;(def len-meter (meter measure-len
;                      (Len-Meter. 0)
;                      #(Len-Meter. (+ (:len %1) (:len %2)))))


(defn -main [& args]
  (when (< (count args) 3)
    (printf "usage: %s <n=size> <m=test id> <l>\n" *file*)
    (printf "(count args)=%d\n" (count args))
    (flush)
    (. System (exit 1)))
  (let [n (. Integer valueOf ^String (nth args 0))
        m (. Integer valueOf ^String (nth args 1))
        l (. Integer valueOf ^String (nth args 2))]
    (printf "n=%d m=%d l=%d\n" n m l)
    (flush)
    (let [list-of-stuff
          (doall
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
               17 (loop [s (sorted-set)
                         i (int n)]
                    (if (zero? i)
                      s
                      (recur (conj s
                                   (.nextGaussian #^java.util.Random r))
                             (dec i))))
               (comment
               18 (loop [s (finger-tree nil)
                         i (int n)]
                    (if (zero? i)
                      s
                      (recur (conjr s
                                    (.nextGaussian #^java.util.Random r))
                             (dec i))))
               19 (let [half (double 0.5)]
                    (loop [s (finger-tree nil)
                           i (int n)]
                      (if (zero? i)
                        s
                        (let [x (.nextGaussian #^java.util.Random r)]
                          (recur (if (< x half)
                                   (conjr s x)
                                   (consl s x))
                                 (dec i))))))
               20 (let [my-len-meter len-meter]
                    (loop [s (finger-tree my-len-meter)
                           i (int n)]
                      (if (zero? i)
                        s
                        (recur (conjr s
                                      (.nextGaussian #^java.util.Random r))
                               (dec i)))))
               )
               21 (random-boxed-double-array n)
               ))]

      ;; Stick around, so we can attach things to it like jmap
      
      (println (format "(count list-of-stuff)=%d" (count list-of-stuff)))
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
                (recur (inc i))))))))
