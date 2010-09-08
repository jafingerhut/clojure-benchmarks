;;   The Computer Language Benchmarks Game
;;   http://shootout.alioth.debian.org/

;; contributed by Andy Fingerhut

(ns fasta
  (:gen-class))

(set! *warn-on-reflection* true)


(defn make-repeat-fasta [#^java.io.BufferedWriter wrtr
                         line-length id desc s n]
  (let [descstr (str ">" id " " desc "\n")]
    (.write wrtr descstr))
  (let [s-len (int (count s))
        line-length (int line-length)
        min-buf-len (int (+ s-len line-length))
        repeat-count (int (inc (quot min-buf-len s-len)))
        buf (apply str (repeat repeat-count s))
        ;; Precompute all strings that we might want to print, one at
        ;; each possible offset in the string s to be repeated.
        line-strings (vec (map (fn [i]
                                 (str (subs buf i (+ i line-length)) "\n"))
                               (range 0 s-len)))
        num-full-lines (int (quot n line-length))]
    (loop [j (int 0)
           s-offset (int 0)
           ]
      (if (== j num-full-lines)
        ;; Write out the left over part of length n, if any.
        (when (not= 0 (rem n line-length))
          (.write wrtr (str (subs buf s-offset (+ s-offset (rem n line-length)))
                            "\n")))
        (do
          (.write wrtr #^String (line-strings s-offset))
          (recur (inc j) (int (rem (+ s-offset line-length) s-len))))))))


(defn make-random-fasta [#^java.io.BufferedWriter wrtr
                         line-length id desc n genelist]
  (let [descstr (str ">" id " " desc "\n")]
    (.write wrtr descstr))
  )


(def alu (str "GGCCGGGCGCGGTGGCTCACGCCTGTAATCCCAGCACTTTGG"
              "GAGGCCGAGGCGGGCGGATCACCTGAGGTCAGGAGTTCGAGA"
              "CCAGCCTGGCCAACATGGTGAAACCCCGTCTCTACTAAAAAT"
              "ACAAAAATTAGCCGGGCGTGGTGGCGCGCGCCTGTAATCCCA"
              "GCTACTCGGGAGGCTGAGGCAGGAGAATCGCTTGAACCCGGG"
              "AGGCGGAGGTTGCAGTGAGCCGAGATCGCGCCACTGCACTCC"
              "AGCCTGGGCGACAGAGCGAGACTCCGTCTCAAAAA"))


(defn -main [& args]
  (let [n (if (and (>= (count args) 1)
                   (re-matches #"^\d+$" (nth args 0)))
            (. Integer valueOf (nth args 0) 10))
        line-length 60
        wrtr (java.io.BufferedWriter. *out*)]
    (make-repeat-fasta wrtr line-length "ONE" "Homo sapiens alu" alu (* 2 n))
;    (make-random-fasta wrtr line-length "TWO" "IUB ambiguity codes" (* 3 n) iub)
;    (make-random-fasta wrtr line-length "THREE" "Homo sapiens frequency" (* 5 n)
;                       homosapiens)
    (.flush wrtr))
  (. System (exit 0)))
