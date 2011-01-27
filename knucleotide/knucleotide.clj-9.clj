;;   The Computer Language Benchmarks Game
;;   http://shootout.alioth.debian.org/

;; contributed by Andy Fingerhut

(ns knucleotide
  (:gen-class))

(set! *warn-on-reflection* true)


;; Return true when the line l is a FASTA description line

(defn fasta-description-line
  [l]
  (= \> (first (seq l))))


;; Return true when the line l is a FASTA description line that begins
;; with the string desc-str.

(defn fasta-description-line-beginning
  [desc-str l]
  (and (fasta-description-line l)
       (= desc-str (subs l 1 (min (count l) (inc (count desc-str)))))))


;; Take a sequence of lines from a FASTA format file, and a string
;; desc-str.  Look for a FASTA record with a description that begins
;; with desc-str, and if one is found, return its DNA sequence as a
;; single (potentially quite long) string.  If input file is big,
;; you'll save lots of memory if you call this function in a with-open
;; for the file, and don't hold on to the head of the lines
;; parameter.

(defn fasta-dna-str-with-desc-beginning
  [desc-str lines]
  (when-let [x (drop-while (fn [l]
                             (not (fasta-description-line-beginning desc-str
                                                                    l)))
                           lines)]
    (when-let [x (seq x)]
      (let [y (take-while (fn [l] (not (fasta-description-line l)))
                          (map (fn [#^java.lang.String s] (.toUpperCase s))
                               (rest x)))]
        (apply str y)))))


;; Returns a sequence of all length len substrings of the string s, if
;; (count s) >= len, otherwise nil.

(defn all-equal-len-subs
  [len s]
  (when (>= (count s) len)
    (map #(subs s % (+ % len)) (range (inc (- (count s) len))))))


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

(defn tally
  [things]
  (persistent! (tally-helper (transient {}) things)))


(defn all-tally-to-str
  [tally]
  (with-out-str
    (let [total (reduce + (vals tally))]
      (doseq [k (sort #(>= (tally %1) (tally %2)) ; sort by tally, largest first
                      (keys tally))]
        (printf "%s %.3f\n" k
                (double (* 100 (/ (tally k) total))))))))


(defn one-tally-to-str
  [key tally]
  (format "%d\t%s" (get tally key 0) key))


(defn -main [& args]
  (with-open [br (java.io.BufferedReader. *in*)]
    (let [dna-str (fasta-dna-str-with-desc-beginning "THREE" (line-seq br))]
      (flush)
      (let [tally1 (all-tally-to-str (tally (all-equal-len-subs 1 dna-str)))]
        (println tally1))
      (flush)
      (let [tally2 (all-tally-to-str (tally (all-equal-len-subs 2 dna-str)))]
        (println tally2))
      (flush)
      (let [tally3 (one-tally-to-str "GGT"
                                     (tally (all-equal-len-subs 3 dna-str)))]
        (println tally3))
      (flush)
      (let [tally4 (one-tally-to-str "GGTA"
                                     (tally (all-equal-len-subs 4 dna-str)))]
        (println tally4))
      (flush)
      (let [tally6 (one-tally-to-str "GGTATT"
                                     (tally (all-equal-len-subs 6 dna-str)))]
        (println tally6))
      (flush)
      (let [tally12 (one-tally-to-str "GGTATTTTAATT"
                                      (tally (all-equal-len-subs 12 dna-str)))]
        (println tally12))
      (flush)
      (let [tally18 (one-tally-to-str "GGTATTTTAATTTATAGT"
                                      (tally (all-equal-len-subs 18 dna-str)))]
        (println tally18))
      (flush))))
