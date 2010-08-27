;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: July, 2009

;; This version is fairly slow.  Would be nice to speed it up without
;; getting too crazy in the implementation.

(set! *warn-on-reflection* true)

(ns fannkuch
  (:gen-class)
  (:use [clojure.contrib.combinatorics :only (lex-permutations)]))


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
  "This version of reverse-first-n assumes that 1 <= n <= (count coll).  No guarantees are made of its correctness if this condition is violated.  It does this merely to avoid checking a few conditions, and thus perhaps be a bit faster."
  [n coll]
  (loop [accum-reverse ()
         n (int (dec n))
         remaining (seq coll)]
    (if (zero? n)
      (concat (cons (first remaining) accum-reverse)
              (next remaining))
      (recur (cons (first remaining) accum-reverse)
             (dec n)
             (next remaining)))))


(defn fannkuch-of-permutation [perm]
  (loop [perm perm
	 flips (int 0)]
    (let [first-num (first perm)]
      (if (== 1 first-num)
	flips
	(let [flipped-perm (reverse-first-n-restricted first-num perm)]
	  (recur flipped-perm (inc flips)))))))


;; Adapted from function of the same name in Paul Graham's "On Lisp".
;; I changed the arguments and return value of the function parameter,
;; so changed the name, too.

;;(defn best-by-f
;;  "f should take one argument, an element of sequence s, and should
;;return a value that implements Comparable as a 'rank' of the
;;element (e.g. an integer).  The earliest element that has a value of f
;;strictly larger than any earlier element is returned.  nil is returned
;;if s is empty."
;;  [f s]
;;  (let [s (seq s)]
;;    (if (nil? s)
;;      nil
;;      (loop [wins (first s)
;;             f-wins (f wins)
;;             s (next s)]
;;        (if s
;;          (let [obj (first s)
;;                f-obj (f obj)]
;;            (if (> f-obj f-wins)
;;              (recur obj f-obj (next s))
;;              (recur wins f-wins (next s))))
;;          wins)))))


(defn fannkuch [N]
  (reduce max (map #(fannkuch-of-permutation %)
                   (lex-permutations (range 1 (inc N))))))

;;(defn fannkuch-perm-with-most-flips [N]
;;  (best-by-f #(fannkuch-of-permutation %)
;;             (lex-permutations (range 1 (inc N)))))


(defn usage [exit-code]
  (println (format "usage: %s N" *file*))
  (println (format "    N must be a positive integer"))
  (. System (exit exit-code)))


(defn -main [& args]
  (when (not= (count args) 1)
    (usage 1))
  (when (not (re-matches #"^\d+$" (nth args 0)))
    (usage 1))
  (def N (. Integer valueOf (nth args 0) 10))
  (when (< N 1)
    (usage 1))
  ;; This is quick compared to iterating through all permutations, so do
  ;; it separately.
  (let [fannkuch-order-perms (permutations-in-fannkuch-order N)]
    (doseq [p (take 30 fannkuch-order-perms)]
      (println (apply str p))))
  (println (format "Pfannkuchen(%d) = %d" N (fannkuch N)))
;;  (let [max-fannkuch-perm (fannkuch-perm-with-most-flips N)]
;;    (println (format "Pfannkuchen(%d) = %d  %s" N
;;                     (fannkuch-of-permutation max-fannkuch-perm)
;;                     (str (seq max-fannkuch-perm)))))
  (. System (exit 0)))
