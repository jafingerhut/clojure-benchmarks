;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: Jul 9, 2009

;; Clojure program intended for submission to the "Computer Language
;; Benchmarks Game" web site, for the k-nucleotide problem, described
;; briefly on the bottom of this web page.

;; http://shootout.alioth.debian.org/u32q/benchmark.php?test=knucleotide&lang=all

;; The benchmark is run on a 4-processor machine, and parallelism is
;; encouraged -- if it reduces the start to finish time of the
;; computation, you rank better on the "Elapsed secs" metric than
;; another program that takes longer start-to-finish, even if the
;; other program uses less total CPU computation time.  I will attempt
;; to add some parallelism later, by calculating the tally1, tally2,
;; tally3, ..., tally18 values in parallel before printing them in the
;; desired order (several other benchmark programs do this, e.g. the
;; C++ and C entries).

;; Until then, I'm looking for other suggestions for speeding up the
;; code, or reducing its memory usage.

(ns knucleotide
  (:gen-class))

(set! *warn-on-reflection* true)


(defn fasta-description-line
  "Return true when the line l is a FASTA description line"
  [l]
  (= \> (first (seq l))))


(defn fasta-description-line-beginning
  "Return true when the line l is a FASTA description line that begins with the string desc-str."
  [desc-str l]
  (and (fasta-description-line l)
       (= desc-str (subs l 1 (min (count l) (inc (count desc-str)))))))


(defn fasta-dna-str-with-desc-beginning
  "Take a sequence of lines from a FASTA format file, and a string desc-str.  Look for a FASTA record with a description that begins with desc-str, and if one is found, return its DNA sequence as a single (potentially quite long) string.  If input file is big, you'll save lots of memory if you call this function in a with-open for the file, and don't hold on to the head of the lines parameter."
  [desc-str lines]
  (when-let [x (drop-while (fn [l]
                             (not (fasta-description-line-beginning desc-str l)))
                           lines)]
    (when-let [x (seq x)]
      (let [y (take-while (fn [l] (not (fasta-description-line l)))
                          (map (fn [#^java.lang.String s] (.toUpperCase s))
                               (rest x)))]
        (apply str y)))))


(defn all-equal-len-subs
  "Returns a sequence of all length len substrings of the string s, if (count s) >= len, otherwise nil."
  [len s]
  (when (>= (count s) len)
    (map #(subs s % (+ % len)) (range (inc (- (count s) len))))))


;; Unfortunately, at least with (clojure-version)=1.1.0-alpha-SNAPSHOT
;; and java version 1.5.0_19 on an Intel Mac with OS X 10.5.7,
;; tally-keeps-head keeps a reference to the beginning of the sequence
;; 'things', and thus keeps memory for the entire sequence during the
;; computation, which is too much.

;; Why does it keep the head, when tally-loses-head does not?

(defn tally-keeps-head
  "Take a sequence things, and return a hash map h whose keys are the set of unique objects in the sequence, and where (h obj) is equal to the number of times obj occurs in the sequence."
  [things]
  (loop [h {}
         remaining things]
    (if-let [r (seq remaining)]
      (let [key (first r)]
        (recur (assoc h key (inc (get h key 0))) (rest r)))
      h)))


(defn tally-loses-head-helper
  "Like tally-keeps-head, except requires that you pass in an empty map as the first argument in order to return the same result.  However, it 'loses its head' (see 'Programming Clojure', pp. 139-140), thus using significantly less memory than tally-keeps-head if no one else is keeping a reference to the head of 'things'."
  [h things]
  (if-let [r (seq things)]
    (let [key (first r)]
      (recur (assoc h key (inc (get h key 0))) (rest r)))
    h))


(defn tally-loses-head
  "Same caller interface as tally-keeps-head, but it uses same memory as weird-tally-loses-head."
  [things]
  (tally-loses-head-helper {} things))


(defn all-tally-to-str
  [tally]
  (with-out-str
    (let [total (reduce + (vals tally))]
      (doseq [k (sort #(>= (tally %1) (tally %2))  ; sort by tally, largest first
                      (keys tally))]
        (println (format "%s %.3f" k
                         (double (* 100 (/ (tally k) total)))))))))


(defn one-tally-to-str
  [key tally]
  (format "%d\t%s" (get tally key 0) key))


(defn compute-one-part
  [dna-str part]
  (condp = part
    0 (all-tally-to-str (tally-loses-head (all-equal-len-subs 1 dna-str)))
    1 (all-tally-to-str (tally-loses-head (all-equal-len-subs 2 dna-str)))
    2 (one-tally-to-str "GGT"
			(tally-loses-head (all-equal-len-subs 3 dna-str)))
    3 (one-tally-to-str "GGTA"
			(tally-loses-head (all-equal-len-subs 4 dna-str)))
    4 (one-tally-to-str "GGTATT"
			(tally-loses-head (all-equal-len-subs 6 dna-str)))
    5 (one-tally-to-str "GGTATTTTAATT"
			(tally-loses-head (all-equal-len-subs 12 dna-str)))
    6 (one-tally-to-str "GGTATTTTAATTTATAGT"
			(tally-loses-head (all-equal-len-subs 18 dna-str)))))


;; Oddly enough, this program runs *faster* on my 2-core Intel Core 2
;; Duo Mac if I use 'map' instead of 'pmap' below.  Perhaps running 4
;; threads in parallel (pmap currently runs 2 more in parallel than
;; there are physical cores) causes the working set to not fit into
;; the processor cache any more, and thus it is slower?

;; I should try to reduce the amount of garbage generated by modifying
;; the code, and then compare with and without parallelism again.

;; with pmap, on the input file that is the output of the fasta
;; benchmark with N=1,000,000:

;; real	1m57.244s
;; user	3m27.638s
;; sys	0m4.911s

;; with map, same input file and everything else:

;; real	1m6.626s
;; user	1m34.041s
;; sys	0m1.553s

(defn -main [& args]
  (with-open [br (java.io.BufferedReader. *in*)]
    (let [dna-str (fasta-dna-str-with-desc-beginning "THREE" (line-seq br))
          results (reverse (pmap #(compute-one-part dna-str %)
                                 (reverse (range 7))))]
      (doseq [r results]
        (println r)
        (flush))))
  (. System (exit 0)))
