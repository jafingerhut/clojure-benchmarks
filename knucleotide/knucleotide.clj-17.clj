;;   The Computer Language Benchmarks Game
;;   http://shootout.alioth.debian.org/

;; contributed by Andy Fingerhut

(ns knucleotide
  (:gen-class))

(set! *warn-on-reflection* true)


(definterface IByteString
  (calculateHash [buf offset])
  (incCount [])
  (^int hashCode [])
  (^int getCount [])
  (^boolean equals [obj2])
  (^String toString [])
  )


(deftype ByteString [^{:unsynchronized-mutable true :tag bytes} byteArr
                     ^{:unsynchronized-mutable true :tag int} hash
                     ^{:unsynchronized-mutable true :tag int} cnt
                     ]
  IByteString
  (calculateHash [this b offset]
    (let [^bytes buf b
          len (int (alength byteArr))]
      (loop [i (int 0)
             offset (int offset)
             temp (int 0)]
        (if (== i len)
          (set! hash temp)
          ;; else
          (let [b (int (aget buf offset))
                bb (byte b)]
            (aset byteArr i bb)
            (recur (unchecked-inc i) (unchecked-inc offset)
                   (unchecked-add (unchecked-multiply temp 31) b)))))))
  (incCount [this]
    (set! cnt (unchecked-inc cnt)))
  (hashCode [this]
    hash)
  (getCount [this]
    cnt)
  (equals [this obj2]
    (let [^ByteString obj2 obj2
          ^bytes byteArr2 (.byteArr obj2)]
      (java.util.Arrays/equals byteArr byteArr2)))
  (toString [this]
    (apply str (map char (seq byteArr))))
  )


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


(defn tally-dna-subs-with-len [len ^bytes dna-bytes]
  (let [last-index (inc (- (alength dna-bytes) len))
        ;;last-index (- (alength dna-bytes) len)
        tally (java.util.HashMap.)]
;;    (println "tally-dna-subs-with-len len=" len
;;             " (alength dna-bytes)=" (alength dna-bytes)
;;             " last-index=" last-index
;;             )
    (loop [offset (int 0)
           ;;offset (int last-index)
	   key (ByteString. (byte-array len) 0 1)]
;;      (if (neg? offset)
      (if (== offset last-index)
        tally
        (do
          (.calculateHash key dna-bytes offset)
;;          (print "key " offset " to search: byteArr=" (.toString key)
;;                 " hash=" (.hashCode key)
;;                 " count=" (.getCount key)
;;                 " -- "
;;                 )
          (if-let [^ByteString found-key (get tally key)]
            (do
              (.incCount found-key)
;;              (println "found key: byteArr=" (.toString found-key)
;;                       " count=" (.getCount found-key))
              (recur (unchecked-inc offset) key))
            (do
              (.put tally key key)
;;              (println "no key found: inserted it")
              (recur (unchecked-inc offset)
                     (ByteString. (byte-array len) 0 1)))))))))


(defn getcnt [^ByteString k]
  (.getCount k))


(defn all-tally-to-str [tally]
  (with-out-str
    (let [total (reduce + (map getcnt (vals tally)))
          cmp (fn [k1 k2]
                ;; Return negative integer if k1 should come earlier
                ;; in the sort order than k2, 0 if they are equal,
                ;; otherwise a positive integer.
                (let [v1 (get tally k1)
                      v2 (get tally k2)
                      cnt1 (int (getcnt v1))
                      cnt2 (int (getcnt v2))]
                  (if (not= cnt1 cnt2)
                    (- cnt2 cnt1)
                    (.compareTo (.toString v1) (.toString v2)))))]
      (doseq [^ByteString k
              (sort cmp (keys tally))]
        (printf "%s %.3f\n" (.toString k)
                (double (* 100 (/ (getcnt (get tally k)) total))))))))


(defn ascii-str-to-bytes [^String s]
  (let [result (byte-array (count s))]
    (dotimes [i (count s)]
      (aset result i (byte (int (nth s i)))))
    result))


(defn one-tally-to-str [dna-str tally]
  (let [key-bytes (ascii-str-to-bytes dna-str)
        key (let [init (ByteString. (byte-array (count dna-str)) 0 1)]
              (.calculateHash init key-bytes 0)
              init)
        occurrences (if-let [val (get tally key)]
                      (getcnt val)
                      0)]
    (format "%d\t%s" occurrences dna-str)))


(defn compute-one-part [dna-bytes part]
  (.println System/err (format "Starting part %d" part))
  (let [ret-val
	[part
	 (condp = part
	     0 (all-tally-to-str (tally-dna-subs-with-len 1 dna-bytes))
	     1 (all-tally-to-str (tally-dna-subs-with-len 2 dna-bytes))
	     2 (one-tally-to-str "GGT"
				 (tally-dna-subs-with-len 3 dna-bytes))
	     3 (one-tally-to-str "GGTA"
				 (tally-dna-subs-with-len 4 dna-bytes))
	     4 (one-tally-to-str "GGTATT"
				 (tally-dna-subs-with-len 6 dna-bytes))
	     5 (one-tally-to-str "GGTATTTTAATT"
				 (tally-dna-subs-with-len 12 dna-bytes))
	     6 (one-tally-to-str "GGTATTTTAATTTATAGT"
				 (tally-dna-subs-with-len 18 dna-bytes)))]]
    (.println System/err (format "Finished part %d" part))
    ret-val))


(def *default-modified-pmap-num-threads*
     (+ 2 (.. Runtime getRuntime availableProcessors)))


(defn -main [& args]
  (let [num-threads (if (>= (count args) 1)
                      (. Integer valueOf (nth args 0) 10)
                      *default-modified-pmap-num-threads*)]
    (with-open [br (java.io.BufferedReader. *in*)]
      (let [dna-bytes (ascii-str-to-bytes
                       (fasta-dna-str-with-desc-beginning "THREE" (line-seq br)))
            ;; Select the order of computing parts such that it is
            ;; unlikely that parts 5 and 6 will be computed
            ;; concurrently.  Those are the two that take the most
            ;; memory.  It would be nice if we could specify a DAG for
            ;; which jobs should finish before others begin -- then we
            ;; could prevent those two parts from running
            ;; simultaneously.
            results (map second
                         (sort #(< (first %1) (first %2))
                               (modified-pmap num-threads
                                              #(compute-one-part dna-bytes %)
                                              '(0 5 6 1 2 3 4)
                                              )))]
;;        (dotimes [i (alength dna-bytes)]
;;          (print " " (aget dna-bytes i)))
;;        (println " eol")
        (doseq [r results]
          (println r)
          (flush)))))
  (shutdown-agents))
