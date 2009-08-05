;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: July, 2009

;; This version takes about 3/4 the time for the medium size run as
;; fannkuch.clj-6.clj.  It uses mutable Java arrays of ints wherever
;; possible, in hopes of speeding it up.  I was hoping that would
;; speed it up more than it did.

;;(set! *warn-on-reflection* true)

(ns clojure.benchmark.fannkuch
;;  (:use [clojure.contrib.combinatorics :only (lex-permutations)])
  )


(defn usage [exit-code]
  (println (format "usage: %s N" *file*))
  (println (format "    N must be a positive integer"))
  (. System (exit exit-code)))

(when (not= (count *command-line-args*) 1)
  (usage 1))
(when (not (re-matches #"^\d+$" (nth *command-line-args* 0)))
  (usage 1))
(def N (. Integer valueOf (nth *command-line-args* 0) 10))
(when (< N 1)
  (usage 1))


(defn left-rotate
  "Return a sequence that is the same as s, except that the first n >= 1 elements have been 'rotated left' by 1 position.

  Examples:
  user> (left-rotate '(1 2 3 4) 2)
  (2 1 3 4)
  user> (left-rotate '(1 2 3 4) 3)
  (2 3 1 4)
  user> (left-rotate '(1 2 3 4) 4)
  (2 3 4 1)
  user> (left-rotate '(1 2 3 4) 1)
  (1 2 3 4)"
  [s n]
  (concat (take (dec n) (rest s)) (list (first s)) (drop n s)))


(defn next-perm-in-fannkuch-order [n perm counts]
  (loop [perm perm
         counts counts
         i 1]
    (let [next-perm (left-rotate perm (inc i))
          dec-count (dec (counts i))
          next-i (inc i)]
      (if (zero? dec-count)
        (if (< next-i n)
          (recur next-perm (assoc counts i (inc i)) next-i)
          [nil nil])
        [next-perm (assoc counts i dec-count)]))))


(defn permutations-in-fannkuch-order-helper [n perm counts]
  (lazy-seq
    (let [[next-perm next-counts] (next-perm-in-fannkuch-order n perm counts)]
      (when next-perm
;        (println (str "::next-perm " (vec next-perm)
;                      " next-counts " next-counts "::"))
        (cons next-perm
              (permutations-in-fannkuch-order-helper n next-perm
                                                     next-counts))))))


(defn permutations-in-fannkuch-order [n]
  (lazy-seq
    (let [init-perm (vec (take n (iterate inc 1)))
          init-count init-perm]
      (cons init-perm
            (permutations-in-fannkuch-order-helper n init-perm init-count)))))


(defn reverse-first-n-restricted
  "This procedure assumes that 1 <= n <= (count v), where v is a vector.  No guarantees are made of its correctness if this condition is violated.  It does this merely to avoid checking a few conditions, and thus perhaps be a bit faster."
  [n v]
  (let [n (int n)
        n-to-swap (int (quot n 2))
        n-1 (int (dec n))]
    (loop [v (transient v)
           i (int 0)]
      (if (< i n-to-swap)
        (let [n-1-i (int (- n-1 i))]
          (recur (assoc! v i (v n-1-i) n-1-i (v i))
                 (inc i)))
        (persistent! v)))))


(defn fannkuch-of-permutation [perm]
  (loop [perm perm
	 flips (int 0)]
    (let [first-num (first perm)]
      (if (== 1 first-num)
	flips
        (recur (reverse-first-n-restricted first-num perm)
               (inc flips))))))


(defn first-lex-permutation [n]
  (range 1 (inc n)))


(defn permutation-from-seq [s]
  (vec s))


;; This is copied from clojure.contrib.combinatorics/iter-perm, and
;; then modified to use the recently added transient/persistent!
;; function.

(defn next-lex-permutation
  "v is expected to be a Clojure vector of integers.  Return nil if a is the last permutation of those integers, in lexicographic order, otherwise return a new vector of integers that is the next permutation in lexicographic order."
  [v]
  (let [len (int (count v)),
        v (transient v),
	j (loop [i (int (- len 2))]
	     (cond (== i -1) nil
		   (< (v i) (v (inc i))) i
		   :else (recur (dec i))))]
    (when j
      (let [j (int j),
            vj (int (v j)),
	    l (int (loop [i (dec len)]
                     (if (< vj (v i)) i (recur (dec i)))))]
	(loop [v (assoc! v j (v l) l vj),
               k (int (inc j)),
               l (int (dec len))]
	  (if (< k l)
            (recur (assoc! v k (v l) l (v k)) (inc k) (dec l))
	    (persistent! v)))))))


;; Useful for unit testing, to compare the output of this function
;; against clojure.contrib.combinatorics/lex-permutations

(defn lex-permutations-2 [s]
  (loop [perm-vec (permutation-from-seq s)
         accum []]
    (if perm-vec
      (recur (next-lex-permutation perm-vec) (conj accum perm-vec))
      accum)))


(defn fannkuch [N]
  (loop [perm-vec (permutation-from-seq (first-lex-permutation N))
         maxflips (int 0)]
    (if perm-vec
      (let [curflips (int (fannkuch-of-permutation perm-vec))]
        (recur (next-lex-permutation perm-vec)
               (int (max maxflips curflips))))
      ;; else
      maxflips)))


;; This is quick compared to iterating through all permutations, so do
;; it separately.
(let [fannkuch-order-perms (permutations-in-fannkuch-order N)]
  (doseq [p (take 30 fannkuch-order-perms)]
    (println (apply str p))))

(println (format "Pfannkuchen(%d) = %d" N (fannkuch N)))

;;(let [max-fannkuch-perm (fannkuch-perm-with-most-flips N)]
;;  (println (format "Pfannkuchen(%d) = %d  %s" N
;;                   (fannkuch-of-permutation max-fannkuch-perm)
;;                   (str (seq max-fannkuch-perm)))))

(. System (exit 0))
