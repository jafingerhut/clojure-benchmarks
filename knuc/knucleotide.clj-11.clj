;;   The Computer Language Benchmarks Game
;;   http://shootout.alioth.debian.org/

;; contributed by Andy Fingerhut

(ns knucleotide
  (:gen-class)
  (:require [clojure.string :as str]))

(set! *warn-on-reflection* true)


(defn my-lazy-map [f coll]
  (lazy-seq
    (when-let [s (seq coll)]
      (cons (f (first s)) (my-lazy-map f (rest s))))))


;; modified-pmap is like pmap from Clojure 1.1, but with only as much
;; parallelism as specified by the parameter num-threads.  Uses
;; my-lazy-map instead of map from core.clj, since that version of map
;; can use unwanted additional parallelism for chunked collections,
;; like ranges.

(defn modified-pmap
  ([num-threads f coll]
     (if (== num-threads 1)
       (map f coll)
       (let [n (if (>= num-threads 2) (dec num-threads) 1)
             rets (my-lazy-map #(future (f %)) coll)
             step (fn step [[x & xs :as vs] fs]
                    (lazy-seq
                      (if-let [s (seq fs)]
                        (cons (deref x) (step xs (rest s)))
                        (map deref vs))))]
         (step rets (drop n rets)))))
  ([num-threads f coll & colls]
     (let [step (fn step [cs]
                  (lazy-seq
                    (let [ss (my-lazy-map seq cs)]
                      (when (every? identity ss)
                        (cons (my-lazy-map first ss)
			      (step (my-lazy-map rest ss)))))))]
       (modified-pmap num-threads #(apply f %) (step (cons coll colls))))))



;; Create a type with deftype that is as memory-efficient in storing
;; ASCII strings as byte arrays (versus Java Strings, which require
;; more memory per ASCII character), but can also be used as keys in
;; Clojure maps.  Straight Java arrays do not have an implementation
;; of equals or hashCode that make them useful as keys in Clojure
;; maps.  This is actually a good idea for several reasons, most
;; significant of which is that Java arrays are mutable.  If you
;; inserted an item in a map / hash table with a Java array as a key,
;; and later modified that array, doing what is done below will cause
;; the hashCode of that array to change, and thus you could no longer
;; retreive the item from the map using that array.  It is not obvious
;; how to create an immutable Java array in Java or in Clojure,
;; although there are some attempts at it described here:

;; http://www.javapractices.com/topic/TopicAction.do?Id=29
;; http://mindprod.com/jgloss/immutable.html

;; Under the assumption that we never modify Java arrays used as keys
;; of maps after items are inserted, then java.util.Arrays/hashCode
;; and equals do the desired things on arrays of primitives.

;; Thanks to Stuart Halloway for providing a working deftype
;; expression that achieves this, when my earlier attempts left me
;; frustrated.

(deftype Key [key]
  Object
  (equals [this other]
    (if (= (class this) (class other))
      (java.util.Arrays/equals ^bytes key ^bytes (.key ^Key other))
      false))
  (hashCode [this]
    (java.util.Arrays/hashCode ^bytes key))
  (toString [this]
    (clojure.string/join \, (seq key))))


;; Return true when the line l is a FASTA description line

(defn fasta-description-line [l]
  (= \> (first (seq l))))


;; Return true when the line l is a FASTA description line that begins
;; with the string desc-str.

(defn fasta-description-line-beginning [desc-str l]
  (and (fasta-description-line l)
       (= desc-str (subs l 1 (min (count l) (inc (count desc-str)))))))


(defn char-to-byte [c]
  (let [n (int c)]
    (if (>= n 128)
      (byte (- n 256))
      (byte n))))


;; Take a sequence of lines from a FASTA format file, and a string
;; desc-str.  Look for a FASTA record with a description that begins
;; with desc-str, and if one is found, return its DNA sequence as a
;; single (potentially quite long) Java byte array.  If the input file
;; is big, you'll save lots of memory if you call this function in a
;; with-open for the file, and don't hold on to the head of the lines
;; parameter.

(defn fasta-dna-str-with-desc-beginning [desc-str lines]
  (when-let [x (drop-while
		(fn [l] (not (fasta-description-line-beginning desc-str l)))
                           lines)]
    (when-let [x (seq x)]
      (let [y (take-while (fn [l] (not (fasta-description-line l)))
                          (map (fn [#^java.lang.String s] (.toUpperCase s))
                               (rest x)))
	    b (new java.io.ByteArrayOutputStream)]
	;; Convert each character of each string in sequence y to a
	;; byte and append it to b.
	(doseq [#^java.lang.String s y]
	  (doseq [c s]
	    (.write b (int (char-to-byte c)))))
	;; return buffer's contents as byte array
	(.toByteArray b)))))


;; Returns a sequence of all length len sub-arrays of the byte array
;; s, if (count s) >= len, otherwise nil.  Each element of the
;; returned sequence is an instance of type Key.

(defn all-equal-len-subs [len s]
  (when (>= (count s) len)
    (map
     ;; #(subs s % (+ % len))
     (fn [i]
       (let [sub-array (byte-array len)]
	 (System/arraycopy s i sub-array 0 len)
	 (Key. sub-array)))
     (range (inc (- (count s) len))))))


(defn dna-str-to-key [s]
  (Key. (byte-array (count s) (map char-to-byte s))))


;; From a byte array key k, construct and return a string

(defn key-to-dna-str [k]
  (apply str (map char (.key k))))


(defn tally-helper
  [h things]
  (if-let [r (seq things)]
    (let [key (first r)]
      (recur (assoc! h key (inc (get h key 0))) (rest r)))
    h))


;; Take a sequence things, and return a hash map h whose keys are the
;; set of unique objects in the sequence, and where (h obj) is equal
;; to the number of times obj occurs in the sequence.  Splitting it
;; into this and a helper function is a little odd, but when I had a
;; more straightforward single function for this in Clojure 1.1 alpha,
;; it 'kept the head' of the sequence and used excessive memory.

(defn tally [things]
  (persistent! (tally-helper (transient {}) things)))


(defn all-tally-to-str [tally fn-key-to-str]
  (with-out-str
    (let [total (reduce + (vals tally))]
      (doseq [k (sort #(>= (tally %1) (tally %2)) ; sort by tally, largest first
                      (keys tally))]
        (printf "%s %.3f\n" (fn-key-to-str k)
                (double (* 100 (/ (tally k) total))))))))


(defn one-tally-to-str [dna-str tally]
  (format "%d\t%s" (get tally (dna-str-to-key dna-str) 0) dna-str))


(defn compute-one-part [dna-str part]
  (.println System/err (format "Starting part %d" part))
  (let [ret-val
	[part
	 (condp = part
	     0 (all-tally-to-str (tally (all-equal-len-subs 1 dna-str))
				 key-to-dna-str)
	     1 (all-tally-to-str (tally (all-equal-len-subs 2 dna-str))
				 key-to-dna-str)
	     2 (one-tally-to-str "GGT"
				 (tally (all-equal-len-subs 3 dna-str)))
	     3 (one-tally-to-str "GGTA"
				 (tally (all-equal-len-subs 4 dna-str)))
	     4 (one-tally-to-str "GGTATT"
				 (tally (all-equal-len-subs 6 dna-str)))
	     5 (one-tally-to-str "GGTATTTTAATT"
				 (tally (all-equal-len-subs 12 dna-str)))
	     6 (one-tally-to-str "GGTATTTTAATTTATAGT"
				 (tally (all-equal-len-subs 18 dna-str))))]]
    (.println System/err (format "Finished part %d" part))
    ret-val))


(def *default-modified-pmap-num-threads*
     (+ 2 (.. Runtime getRuntime availableProcessors)))


(defn -main [& args]
  (def num-threads
       (if (and (>= (count args) 1)
		(re-matches #"^\d+$" (nth args 0)))
	 (let [n (. Integer valueOf (nth args 0) 10)]
	   (if (== n 0)
	     *default-modified-pmap-num-threads*
	     n))
         *default-modified-pmap-num-threads*))
  (with-open [br (java.io.BufferedReader. *in*)]
    (let [dna-str (fasta-dna-str-with-desc-beginning "THREE" (line-seq br))
          ;; Select the order of computing parts such that it is
          ;; unlikely that parts 5 and 6 will be computed concurrently.
          ;; Those are the two that take the most memory.  It would be
          ;; nice if we could specify a DAG for which jobs should finish
          ;; before others begin -- then we could prevent those two
          ;; parts from running simultaneously.
          results (map second
                       (sort #(< (first %1) (first %2))
                             (modified-pmap num-threads
                                            #(compute-one-part dna-str %)
                                            ;; '(6 0 1 2 3 4 5)
                                            '(0 5 6 1 2 3 4)
					    )))]
      (doseq [r results]
        (println r)
        (flush))))
  (. System (exit 0)))
