;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: July, 2009

;; This version is only slightly faster than fannkuch.clj-6.clj, if at
;; all.  I should probably try another version that uses mutable Java
;; arrays for iterating through the sequence of permutations, instead
;; of using lex-permutations.

;;(set! *warn-on-reflection* true)

(ns clojure.benchmark.fannkuch
  (:use [clojure.contrib.combinatorics :only (lex-permutations)])
  )


(defn usage [exit-code]
  (println (format "usage: %s N" *file*))
  (println (format "    N must be a positive integer"))
  (. System (exit exit-code)))

(when (not= (count *command-line-args*) 1)
  (usage 1))
(def N
     (let [arg (nth *command-line-args* 0)]
       (when (not (re-matches #"^\d+$" arg))
         (usage 1))
       (let [temp (. Integer valueOf arg 10)]
         (when (< temp 1)
           (usage 1))
         temp)))


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


(defn reverse-first-n!
  "This procedure assumes that 1 <= n <= (alength java-arr).  No guarantees are made of its correctness if this condition is violated.  It does this merely to avoid checking a few conditions, and thus perhaps be a bit faster."
  [n java-arr]
  (let [n (int n)
        limit (int (quot (inc n) 2))
        n-1 (int (dec n))]
    (loop [i (int 0)]
      (when (<= i limit)
        (let [temp (aget java-arr i)
              n-1-i (int (- n-1 i))]
          (aset java-arr i (aget java-arr n-1-i))
          (aset java-arr n-1-i temp))))))


;; fannkuch-of-permutation is purely functional "as viewed from the
;; outside", since it takes as input the collection perm, and then
;; returns an integer that is a function of the value of the
;; collection only.

;; The fact that it is a pure function might be difficult to determine
;; via a program, since it calls reverse-first-n!, which is definitely
;; not functional.  It mutates its parameter perm-arr.  However,
;; fannkuch-of-permutation creates that array, and never returns it or
;; uses it in any other way than mutating it "locally".

(defn fannkuch-of-permutation [perm]
  (if (== 1 (first perm))
    ;; Handle this special case without bothering to create a Java
    ;; array.
    0
    (let [perm-arr (into-array Integer/TYPE perm)]
      (loop [flips (int 0)]
        (let [first-num (aget perm-arr 0)]
          (if (== 1 first-num)
            flips
            (do
              (reverse-first-n! first-num perm-arr)
              (recur (inc flips)))))))))


(defn fannkuch [N]
  (let [perms (lex-permutations (range 1 (inc N)))]
    (loop [s (seq perms)
	   maxflips (int 0)]
      (if s
	(let [perm (first s)]
	  (let [curflips (int (fannkuch-of-permutation perm))]
	    (recur (seq (rest s))
                   (int (max maxflips curflips)))))
	;; else
	maxflips))))


;; This is quick compared to iterating through all permutations, so do
;; it separately.
(let [fannkuch-order-perms (permutations-in-fannkuch-order N)]
  (doseq [p (take 30 fannkuch-order-perms)]
    (println (apply str p))))

(println (format "Pfannkuchen(%d) = %d" N (fannkuch N)))

(. System (exit 0))
