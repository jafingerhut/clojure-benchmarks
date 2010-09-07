;;   The Computer Language Benchmarks Game
;;   http://shootout.alioth.debian.org/

;; contributed by Andy Fingerhut

(ns fannkuchredux
  (:require clojure.string)
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


(defmacro rotate-left-first-n! [n #^ints a]
  `(let [n# (int ~n)
	 n-1# (dec n#)
	 a0# (aget ~a 0)]
     (loop [i# (int 0)]
       (if (== i# n-1#)
	 (aset ~a n-1# a0#)
	 (let [i+1# (inc i#)]
	   (aset ~a i# (aget ~a i+1#))
	   (recur i+1#))))))


(defn fannkuch-of-permutation [#^ints p]
  (if (== 1 (aget p 0))
    ;; Handle this special case without bothering to create a Java
    ;; array.
    0
    ;; Using aclone instead of copy-java-int-array was a big
    ;; improvement.
    (let [p2 (aclone p)]
      (loop [flips (int 0)]
        (let [first-num (int (aget p2 0))]
          (if (== 1 first-num)
            flips
            (do
              (reverse-first-n! first-num p2)
              (recur (inc flips)))))))))


;; initialize the permutation generation algorithm.  The permutations
;; need to be generated in a particular order so that the checksum may
;; be computed correctly (or if you can determine some way to
;; calculate the sign from an arbitrary permutation, then you can
;; generate the permutations in any order you wish).

(defn init-permutation [n]
  [(int-array (range 1 (inc n)))    ;; permutation
   1                                ;; sign
   (int-array (range 1 (inc n)))])  ;; array of count values


(defmacro swap-array-elems! [a i j]
  `(let [temp# (aget ~a ~i)]
     (aset ~a ~i (aget ~a ~j))
     (aset ~a ~j temp#)))


;; Modify the passed Java arrays p (a permutation) and c (count
;; values) in place.  Let caller negate the sign themselves.

;; Return true if the final value of p is a new permutation, false if
;; there are no more permutations and the caller should not use the
;; value of p for anything.

(defn next-permutation! [N #^ints p sign #^ints c]
  (if (neg? sign)
    (let [N (int N)
	  N-1 (dec N)]
      (swap-array-elems! p 1 2)
      (loop [i (int 2)]
	(if (== i N)
	  true)
	(let [cx (aget c i)
	      i+1 (inc i)]
	  (if (not= cx 1)
	    (do
	      (aset c i (dec cx))
	      true)
	    (if (== i N-1)
	      false
	      (do
		(aset c i i+1)
		(rotate-left-first-n! (inc i+1) p)
		(recur i+1)))))))
    (swap-array-elems! p 0 1)))


(defn fannkuch [N]
  (let [[#^ints p first-sign #^ints c] (init-permutation N)]
    (loop [sign (int first-sign)
	   maxflips (int 0)
	   checksum (int 0)]
      (let [curflips (int (fannkuch-of-permutation p))
	    next-maxflips (int (max maxflips curflips))
            next-checksum (+ checksum (* sign curflips))
            next-sign (int (- sign))]
;;	(print (clojure.string/join "" (seq p)) " "
;;	       (clojure.string/join "" (seq c)))
;;	(if (zero? curflips)
;;	  (printf " ----- --\n")
;;	  (printf " %5d %2d %5d\n" curflips sign next-checksum))
	(if (next-permutation! N p sign c)
	  (recur next-sign next-maxflips next-checksum)
	  [next-checksum next-maxflips])))))


(defn -main [& args]
  (let [N (if (and (>= (count args) 1)
		   (re-matches #"^\d+$" (nth args 0)))
	    (. Integer valueOf (nth args 0) 10)
	    10)]
    (let [[checksum maxflips] (fannkuch N)]
      (printf "%d\n" checksum)
      (printf "Pfannkuchen(%d) = %d\n" N maxflips)))
  (flush)
  (. System (exit 0)))
