;;   The Computer Language Benchmarks Game
;;   http://shootout.alioth.debian.org/

;; contributed by Andy Fingerhut

(ns fannkuchredux
  (:gen-class))

(set! *warn-on-reflection* true)


;; This macro assumes that 1 <= n <= (alength a), where a is a Java
;; array of ints.  No guarantees are made of its correctness if this
;; condition is violated.  It does this merely to avoid checking a few
;; conditions, and thus perhaps be a bit faster.

(defmacro reverse-first-n! [n #^ints a]
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
  (into-array Integer/TYPE s))


;; next-lex-permutation! is copied from
;; clojure.contrib.combinatorics/iter-perm, and then modified so that
;; the parameter is a Java int array, and this procedure modifies it
;; in place.

;; Modify the parameter a, expected to be a Java array of ints so that
;; it becomes the next permutation in lexicographic order.  Return the
;; array, or nil if the input array was the last one in lexicographic
;; order.

(defn next-lex-permutation! [#^ints a]
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
         maxflips (int 0)
         checksum (int 0)
         sign (int 1)]
    (if perm-arr
      (let [curflips (int (fannkuch-of-permutation perm-arr))
            next-checksum (+ checksum (* sign curflips))
            next-sign (int (- sign))]
        (recur (next-lex-permutation! perm-arr)
               (int (max maxflips curflips))
               next-checksum next-sign))
      [checksum maxflips])))


(defn -main [& args]
  (def N (if (and (>= (count args) 1)
                  (re-matches #"^\d+$" (nth args 0)))
           (. Integer valueOf (nth args 0) 10)
           10))
  (let [[checksum maxflips] (fannkuch N)]
    (println (format "%d" checksum))
    (println (format "Pfannkuchen(%d) = %d" N maxflips)))
  (. System (exit 0)))
