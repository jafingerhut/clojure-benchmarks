;;   The Computer Language Benchmarks Game
;;   http://shootout.alioth.debian.org/

;; contributed by Andy Fingerhut

(ns fannkuch
  (:gen-class))

(set! *warn-on-reflection* true)


(defn left-rotate
  "Return a sequence that is the same as s, except that the first n >=
1 elements have been 'rotated left' by 1 position.

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
  "This macro assumes that 1 <= n <= (alength a), where a is a Java
array of ints.  No guarantees are made of its correctness if this
condition is violated.  It does this merely to avoid checking a few
conditions, and thus perhaps be a bit faster."
  [n #^ints a]
  `(let [n# (int ~n)
         n-1# (int (dec n#))]
     (loop [i# (int 0)
            j# (int n-1#)]
       (when (< i# j#)
         (let [temp# (aget ~a i#)]
           (aset ~a i# (aget ~a j#))
           (aset ~a j# temp#))
         (recur (inc i#) (dec j#))))))


(defn fannkuch-of-permutation [#^ints perm]
  (if (== 1 (aget perm 0))
    ;; Handle this special case without bothering to create a Java
    ;; array.
    0
    ;; Using aclone instead of copy-java-int-array was a big
    ;; improvement.
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
  (int-array (count s) s))


;; next-lex-permutation! is copied from
;; clojure.contrib.combinatorics/iter-perm, and then modified so that
;; the parameter is a Java int array, and this procedure modifies it
;; in place.

(defn next-lex-permutation!
  "Modify the parameter a, expected to be a Java array of ints so that
it becomes the next permutation in lexicographic order.  Return the
array, or nil if the input array was the last one in lexicographic
order."
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


(defn fannkuch [N]
  (loop [perm-arr (permutation-from-seq (first-lex-permutation N))
         maxflips (int 0)]
    (if perm-arr
      (let [curflips (int (fannkuch-of-permutation perm-arr))]
        (recur (next-lex-permutation! perm-arr)
               (int (max maxflips curflips))))
      ;; else
      maxflips)))


(defn usage [exit-code]
  (printf "usage: %s N\n" *file*)
  (printf "    N must be a positive integer\n")
  (flush)
  (. System (exit exit-code)))


(defn -main [& args]
  (when (not= (count args) 1)
    (usage 1))
  (def N
       (let [arg (nth args 0)]
         (when (not (re-matches #"^\d+$" arg))
           (usage 1))
         (let [temp (. Integer valueOf arg 10)]
           (when (< temp 1)
             (usage 1))
           temp)))
  (let [fannkuch-order-perms (permutations-in-fannkuch-order N)]
    (doseq [p (take 30 fannkuch-order-perms)]
      (println (apply str p))))
  (printf "Pfannkuchen(%d) = %d\n" N (fannkuch N))
  (flush))
