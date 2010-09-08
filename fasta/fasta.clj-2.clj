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
           s-offset (int 0)]
      (if (== j num-full-lines)
        ;; Write out the left over part of length n, if any.
        (when (not= 0 (rem n line-length))
          (.write wrtr (str (subs buf s-offset (+ s-offset (rem n line-length)))
                            "\n")))
        (do
          (.write wrtr #^String (line-strings s-offset))
          (recur (inc j) (int (rem (+ s-offset line-length) s-len))))))))


(def last-random (int 42))

(let [IM (int 139968)
      IA (int 3877)
      IC (int 29573)]
  (defn gen-random! [max]
    (let [max (double max)
          last-rand (int last-random)
          next-rand (int (rem (+ (* last-rand IA) IC) IM))]
      (set! last-random next-rand)
      (/ (* max next-rand) IM))))


;; Find desired gene from cdf using binary search.

(defn lookup-gene [genelist rand-frac]
  (let [#^chars chars (:chars genelist)
        #^doubles cdf (:cdf genelist)
        n (alength cdf)
        x (double rand-frac)]
    (loop [lo (int -1)
           hi (int (dec n))]
      (if (== (inc lo) hi)
        (aget chars hi)
        (let [mid (int (quot (+ lo hi) 2))]
          (if (< x (aget cdf mid))
            (recur lo mid)
            (recur mid hi)))))))


(defn fill-random! [genelist n #^chars buf]
  (let [max-val (double 1.0)]
    (dotimes [i n]
      (aset buf i (char (lookup-gene genelist (gen-random! max-val)))))))


(defn make-random-fasta [#^java.io.BufferedWriter wrtr
                         line-length id desc n genelist]
  (let [descstr (str ">" id " " desc "\n")]
    (.write wrtr descstr))
  (let [line-length (int line-length)
        len-with-newline (int (inc line-length))
        num-full-lines (int (quot n line-length))
        line-buf (char-array len-with-newline)]
    (aset line-buf line-length \newline)
    (dotimes [i num-full-lines]
      (fill-random! genelist line-length line-buf)
      (.write wrtr line-buf (int 0) len-with-newline))
    (let [remaining-len (int (rem n line-length))]
      (when (not= 0 remaining-len)
        (fill-random! genelist remaining-len line-buf)
        (.write wrtr line-buf 0 remaining-len)
        (.write wrtr "\n")))))


(def alu (str "GGCCGGGCGCGGTGGCTCACGCCTGTAATCCCAGCACTTTGG"
              "GAGGCCGAGGCGGGCGGATCACCTGAGGTCAGGAGTTCGAGA"
              "CCAGCCTGGCCAACATGGTGAAACCCCGTCTCTACTAAAAAT"
              "ACAAAAATTAGCCGGGCGTGGTGGCGCGCGCCTGTAATCCCA"
              "GCTACTCGGGAGGCTGAGGCAGGAGAATCGCTTGAACCCGGG"
              "AGGCGGAGGTTGCAGTGAGCCGAGATCGCGCCACTGCACTCC"
              "AGCCTGGGCGACAGAGCGAGACTCCGTCTCAAAAA"))

(def iub [[\a 0.27]
          [\c 0.12]
          [\g 0.12]
          [\t 0.27]
          [\B 0.02]
          [\D 0.02]
          [\H 0.02]
          [\K 0.02]
          [\M 0.02]
          [\N 0.02]
          [\R 0.02]
          [\S 0.02]
          [\V 0.02]
          [\W 0.02]
          [\Y 0.02]])

(def homosapiens [[\a 0.3029549426680]
                  [\c 0.1979883004921]
                  [\g 0.1975473066391]
                  [\t 0.3015094502008]])


(defn prefix-sums-helper [x coll]
  (lazy-seq
   (when-let [s (seq coll)]
     (let [sum (+ x (first s))]
       (cons sum (prefix-sums-helper sum (rest s)))))))


(defn prefix-sums [coll]
  (prefix-sums-helper 0 coll))


(defn make-genelist [pdf-map]
  (let [n (count pdf-map)
        chars (char-array n (map first pdf-map))
        cdf (double-array n (prefix-sums (map #(nth % 1) pdf-map)))]
    {:chars chars, :cdf cdf}))


(defn -main [& args]
  (let [n (if (and (>= (count args) 1)
                   (re-matches #"^\d+$" (nth args 0)))
            (. Integer valueOf (nth args 0) 10))
        line-length 60
        wrtr (java.io.BufferedWriter. *out*)
        iub-genelist (make-genelist iub)
        homosapiens-genelist (make-genelist homosapiens)]
    (binding [last-random (int 42)]
      (make-repeat-fasta wrtr line-length "ONE" "Homo sapiens alu" alu (* 2 n))
      (make-random-fasta wrtr line-length "TWO" "IUB ambiguity codes"
                         (* 3 n) iub-genelist)
      (make-random-fasta wrtr line-length "THREE" "Homo sapiens frequency"
                         (* 5 n) homosapiens-genelist))
    (.flush wrtr))
  (. System (exit 0)))
