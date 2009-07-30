;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: July, 2009

;;(set! *warn-on-reflection* true)

;; TBD: clojure.contrib.combinatorics's lex-permutations returns the
;; permutations in a different order than that needed by this
;; benchmark for printing the first 30 permutations.  Need to replace
;; that with a modified version.

;; This version is also fairly slow.  Would be nice to speed it up
;; without getting too crazy in the implementation.


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


(use 'clojure.contrib.combinatorics)


(defn fannkuch-of-permutation [perm]
  (loop [perm perm
	 flips (int 0)]
    (let [first-num (first perm)]
      (if (== 1 first-num)
	flips
	(let [flipped-perm (into (vec (reverse (subvec perm 0 first-num)))
				 (subvec perm first-num))]
	  (recur flipped-perm (inc flips)))))))


(defn fannkuch [N]
  (let [perms (lex-permutations (range 1 (inc N)))]
    (loop [s (seq perms)
	   iter (int 0)
	   maxflips (int 0)]
      (if s
	(let [perm (first s)]
	  (when (< iter 30)
	    (println (apply str perm)))
	  (let [curflips (int (fannkuch-of-permutation perm))]
	    (recur (seq (rest s)) (inc iter) (int (max maxflips curflips)))))
	;; else
	maxflips))))


(println (format "Pfannkuchen(%d) = %d" N (fannkuch N)))

(. System (exit 0))
