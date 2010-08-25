;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: July, 2009

;; This version takes about 3/4 the time for the medium size run as
;; fannkuch.clj-6.clj.  It uses mutable Java arrays of ints wherever
;; possible, in hopes of speeding it up.  I was hoping that would
;; speed it up more than it did.

(ns clojure.benchmark.fannkuch
;;  (:use [clojure.contrib.combinatorics :only (lex-permutations)])
  )

(set! *warn-on-reflection* true)


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


(defmacro reverse-first-n!
  "This macro assumes that 1 <= n <= (alength a), where a is a Java array of ints.  No guarantees are made of its correctness if this condition is violated.  It does this merely to avoid checking a few conditions, and thus perhaps be a bit faster."
  [n #^ints a]
  `(let [n# (int ~n)
         n-to-swap# (int (quot n# 2))
         n-1# (int (dec n#))]
     (loop [i# (int 0)]
       (when (< i# n-to-swap#)
         (let [temp# (aget ~a i#)
               n-1-i# (int (- n-1# i#))]
           (aset ~a i# (aget ~a n-1-i#))
           (aset ~a n-1-i# temp#))
         (recur (inc i#))))))


(defn fannkuch-of-permutation [#^ints perm]
  (if (== 1 (aget perm 0))
    ;; Handle this special case without bothering to create a Java
    ;; array.
    0
    ;; Using aclone instead of copy-java-int-array (see
    ;; fannkuch.clj-8.clj) was a big improvement.
    (let [perm-arr (aclone perm)]
      (loop [flips (int 0)]
        (let [first-num (aget perm-arr 0)]
          (if (== 1 first-num)
            flips
            (do
              (reverse-first-n! first-num perm-arr)
              (recur (inc flips)))))))))


(defn first-lex-permutation [n]
  (range 1 (inc n)))


(defn permutation-from-seq [s]
  (into-array Integer/TYPE s))


;; This is copied from clojure.contrib.combinatorics/iter-perm, and
;; then modified so that the parameter is a Java int array, and this
;; procedure modifies it in place.

(defn next-lex-permutation!
  "Modify the parameter a, expected to be a Java array of ints so that it becomes the next permutation in lexicographic order.  Return the array, or nil if the input array was the last one in lexicographic order."
  [#^ints a]
  (let [len (int (alength a)),
	j (loop [i (int (- len 2))]
	     (cond (== i -1) nil
		   (< (aget a i) (aget a (inc i))) i
		   :else (recur (dec i))))]
    (when j
      (let [j (int j),
            vj (int (aget a j)),
	    l (int (loop [i (dec len)]
                     (if (< vj (aget a i)) i (recur (dec i)))))]
        (aset a j (aget a l))
        (aset a l vj)
	(loop [k (int (inc j)),
               l (int (dec len))]
	  (if (< k l)
            (do
              (let [temp (aget a k)]
                (aset a k (aget a l))
                (aset a l temp))
              (recur (inc k) (dec l)))
	    a))))))


;; Useful for unit testing, to compare the output of this function
;; against clojure.contrib.combinatorics/lex-permutations

(comment
(defn lex-permutations-2 [s]
  (loop [#^ints perm-arr (permutation-from-seq s)
         accum []]
    (if perm-arr
      (let [perm-copy (doall (seq (aclone perm-arr)))]
        (recur (next-lex-permutation! perm-arr) (conj accum perm-copy)))
      accum)))
)


(defn fannkuch [N]
  (loop [perm-arr (permutation-from-seq (first-lex-permutation N))
         maxflips (int 0)]
    (if perm-arr
      (let [curflips (int (fannkuch-of-permutation perm-arr))]
        (recur (next-lex-permutation! perm-arr)
               (int (max maxflips curflips))))
      ;; else
      maxflips)))


;;(defn fannkuch-perm-with-most-flips [N]
;;  (loop [perm-arr (permutation-from-seq (first-lex-permutation N))
;;         arr-with-maxflips nil
;;         maxflips (int 0)]
;;    (if perm-arr
;;      (let [curflips (int (fannkuch-of-permutation perm-arr))]
;;        (if (> curflips maxflips)
;;          (let [copy (copy-java-int-array perm-arr)]
;;            (recur (next-lex-permutation! perm-arr) copy curflips))
;;          (recur (next-lex-permutation! perm-arr) arr-with-maxflips maxflips)))
;;      arr-with-maxflips)))


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
