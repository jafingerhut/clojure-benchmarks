;;   The Computer Language Benchmarks Game
;;   http://shootout.alioth.debian.org/

;; contributed by Andy Fingerhut

(ns knucleotide
  (:gen-class))

(set! *warn-on-reflection* true)


;; Handle slight difference in function name between Clojure 1.2.0 and
;; 1.3.0-alpha* ability to use type hints to infer fast bit
;; operations.
(defmacro my-unchecked-inc-int [& args]
  (if (and (== (*clojure-version* :major) 1)
           (== (*clojure-version* :minor) 2))
    `(unchecked-inc ~@args)
    `(unchecked-inc-int ~@args)))

(defmacro key-type [num]
  (if (and (== (*clojure-version* :major) 1)
           (== (*clojure-version* :minor) 2))
    num
    `(long ~num)))


(definterface ITallyCounter
  (^int get_count [])
  (inc_BANG_ []))


(deftype TallyCounter [^{:unsynchronized-mutable true :tag int} cnt]
  ITallyCounter
  (get-count [this] cnt)
  (inc! [this]
    (set! cnt (my-unchecked-inc-int cnt))))

 
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


;; Return true when the line l is a FASTA description line

(defn fasta-description-line [l]
  (= \> (first (seq l))))


;; Return true when the line l is a FASTA description line that begins
;; with the string desc-str.

(defn fasta-description-line-beginning [desc-str l]
  (and (fasta-description-line l)
       (= desc-str (subs l 1 (min (count l) (inc (count desc-str)))))))


;; Take a sequence of lines from a FASTA format file, and a string
;; desc-str.  Look for a FASTA record with a description that begins
;; with desc-str, and if one is found, return its DNA sequence as a
;; single (potentially quite long) string.  If input file is big,
;; you'll save lots of memory if you call this function in a with-open
;; for the file, and don't hold on to the head of the lines parameter.

(defn fasta-dna-str-with-desc-beginning [desc-str lines]
  (when-let [x (drop-while
		(fn [l] (not (fasta-description-line-beginning desc-str l)))
		lines)]
    (when-let [x (seq x)]
      (let [y (take-while (fn [l] (not (fasta-description-line l)))
                          (map (fn [#^java.lang.String s] (.toUpperCase s))
                               (rest x)))]
        (apply str y)))))


(def dna-char-to-code-val {\A 0, \C 1, \T 2, \G 3})
(def code-val-to-dna-char {0 \A, 1 \C, 2 \T, 3 \G})


;; In the hash map 'tally' in tally-dna-subs-with-len, it is more
;; straightforward to use a Clojure string (same as a Java string) as
;; the key, but such a key is significantly bigger than it needs to
;; be, increasing memory and time required to hash the value.  By
;; converting a string of A, C, T, and G characters down to an integer
;; that contains only 2 bits for each character, we make a value that
;; is significantly smaller and faster to use as a key in the map.

;;    most                 least
;; significant          significant
;; bits of int          bits of int
;;  |                         |
;;  V                         V
;; code code code ....  code code
;;  ^                         ^
;;  |                         |
;; code for               code for
;; *latest*               *earliest*
;; char in                char in
;; sequence               sequence

;; Note: Given Clojure 1.2's implementation of bit-shift-left/right
;; operations, when the value being shifted is larger than a 32-bit
;; int, they are faster when the shift amount is a compile time
;; constant.

(defn dna-str-to-key [s]
  ;; Accessing a local let binding is much faster than accessing a var
  (let [dna-char-to-code-val dna-char-to-code-val]
    (loop [key 0
	   offset (int (dec (count s)))]
      (if (neg? offset)
	key
	(let [c (nth s offset)
              code (int (dna-char-to-code-val c))
	      new-key (+ (bit-shift-left key 2) code)]
	  (recur new-key (dec offset)))))))


(defn key-to-dna-str [k len]
  (apply str (map code-val-to-dna-char
		  (map (fn [pos] (bit-and 3 (bit-shift-right k pos)))
		       (range 0 (* 2 len) 2)))))


(defn tally-dna-subs-with-len [len dna-str]
  (let [mask-width (* 2 len)
	mask (key-type (dec (bit-shift-left 1 mask-width)))
	dna-char-to-code-val dna-char-to-code-val]
    (loop [offset (int (- (count dna-str) len))
	   key (key-type (dna-str-to-key (subs dna-str offset (+ offset len))))
           tally (let [h (java.util.HashMap.)
                       one (TallyCounter. (int 1))]
                   (.put h key one)
                   h)]
      (if (zero? offset)
        tally
	(let [new-offset (dec offset)
	      new-first-char-code (dna-char-to-code-val
                                   (nth dna-str new-offset))
	      new-key (key-type (bit-and mask (+ (bit-shift-left key 2)
                                                 new-first-char-code)))]
          (if-let [^TallyCounter cur-count (get tally new-key)]
            (.inc! cur-count)
            (let [one (TallyCounter. (int 1))]
              (.put tally new-key one)))
	  (recur new-offset new-key tally))))))


(defn getcnt [^TallyCounter tc]
  (.get-count tc))


(defn all-tally-to-str [tally fn-key-to-str]
  (with-out-str
    (let [total (reduce + (map getcnt (vals tally)))
          cmp-keys (fn [k1 k2]
                     ;; Return negative integer if k1 should come earlier
                     ;; in the sort order than k2, 0 if they are equal,
                     ;; otherwise a positive integer.
                     (let [cnt1 (int (getcnt (get tally k1)))
                           cnt2 (int (getcnt (get tally k2)))]
                       (if (not= cnt1 cnt2)
                         (- cnt2 cnt1)
                         (let [^String s1 (fn-key-to-str k1)
                               ^String s2 (fn-key-to-str k2)]
                           (.compareTo s1 s2)))))]
      (doseq [k (sort cmp-keys (keys tally))]
        (printf "%s %.3f\n" (fn-key-to-str k)
                (double (* 100 (/ (getcnt (get tally k)) total))))))))


(defn one-tally-to-str [dna-str tally]
  (let [zerotc (TallyCounter. 0)]
    (format "%d\t%s" (getcnt (get tally (dna-str-to-key dna-str) zerotc))
            dna-str)))


(defn compute-one-part [dna-str part]
  [part
   (condp = part
       0 (all-tally-to-str (tally-dna-subs-with-len 1 dna-str)
                           (fn [k] (key-to-dna-str k 1)))
       1 (all-tally-to-str (tally-dna-subs-with-len 2 dna-str)
                           (fn [k] (key-to-dna-str k 2)))
       2 (one-tally-to-str "GGT"
                           (tally-dna-subs-with-len 3 dna-str))
       3 (one-tally-to-str "GGTA"
                           (tally-dna-subs-with-len 4 dna-str))
       4 (one-tally-to-str "GGTATT"
                           (tally-dna-subs-with-len 6 dna-str))
       5 (one-tally-to-str "GGTATTTTAATT"
                           (tally-dna-subs-with-len 12 dna-str))
       6 (one-tally-to-str "GGTATTTTAATTTATAGT"
                           (tally-dna-subs-with-len 18 dna-str)))])


(def +default-modified-pmap-num-threads+
     (+ 2 (.. Runtime getRuntime availableProcessors)))


(defn -main [& args]
  (def num-threads
       (if (and (>= (count args) 1)
		(re-matches #"^\d+$" (nth args 0)))
	 (let [n (. Integer valueOf (nth args 0) 10)]
	   (if (== n 0)
	     +default-modified-pmap-num-threads+
	     n))
         +default-modified-pmap-num-threads+))
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
                                            '(0 5 6 1 2 3 4)
					    )))]
      (doseq [r results]
        (println r)
        (flush))))
  (shutdown-agents))
