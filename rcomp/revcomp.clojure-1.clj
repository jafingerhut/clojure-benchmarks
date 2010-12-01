;;   The Computer Language Benchmarks Game
;;   http://shootout.alioth.debian.org/

;; contributed by Andy Fingerhut

(ns revcomp
  (:gen-class))

(set! *warn-on-reflection* true)


(defn fasta-slurp-br
;   "Reads the supplied BufferedReader using the encoding enc and
;    returns a vector of two strings and a boolean.  The first string is
;    a FASTA description line (without the leading > character), and the
;    second string is the DNA sequence following the description line.
;    The boolean is whether there are more DNA sequences to read from
;    the file after the one returned.
; 
;    Note: It consumes the next > for the next FASTA sequence if there
;    is another one, but subsequent calls to this same function handle
;    that correctly."
  ([#^java.io.BufferedReader r]
     (fasta-slurp-br r (.name (java.nio.charset.Charset/defaultCharset))))
  ([#^java.io.BufferedReader r #^String enc]
     (let [desc-sb (new StringBuilder)
           dna-str-sb (new StringBuilder)
           fasta-desc-line-c (int \>)
           nl-c (int \newline)]
       (.append desc-sb \>)
       (loop [c (int (.read r))
              save-c (not= c fasta-desc-line-c)]
         (cond
           (neg? c)    [(str desc-sb) (str dna-str-sb) false]
           (== c nl-c) nil  ;; finished reading desc line.  Go to next loop.
           :else       (do
                         (when save-c
                           (.append desc-sb (char c)))
                         (recur (int (.read r)) true))))
       (loop [c (int (.read r))]
         (cond
           (neg? c)    [(str desc-sb) (str dna-str-sb) false]
           (== c nl-c) (recur (int (.read r)))
           (== c fasta-desc-line-c)
                       [(str desc-sb) (str dna-str-sb) true]
           :else       (do
                         (.append dna-str-sb (char c))
                         (recur (int (.read r)))))))))


(def complement-dna-char-map
     {\w \W, \W \W,
      \s \S, \S \S,
      \a \T, \A \T,
      \t \A, \T \A,
      \u \A, \U \A,
      \g \C, \G \C,
      \c \G, \C \G,
      \y \R, \Y \R,
      \r \Y, \R \Y,
      \k \M, \K \M,
      \m \K, \M \K,
      \b \V, \B \V,
      \d \H, \D \H,
      \h \D, \H \D,
      \v \B, \V \B,
      \n \N, \N \N })


(defn make-vec-char-mapper [cmap]
  (into [] (map (fn [code]
                  (if (contains? cmap (char code))
                    (int (cmap (char code)))
                    code))
                (range 256))))


(defn println-string-to-buffered-writer [#^java.io.BufferedWriter bw
                                         #^java.lang.String s]
  (. bw write (.toCharArray s) 0 (count s))
  (. bw newLine))


(defn print-reverse-complement-of-str-in-lines [#^java.io.BufferedWriter bw
                                                #^java.lang.String s
                                                complement-fn
                                                max-len]
  (let [comp complement-fn
        len (int (count s))
        max-len (int max-len)]
    (when (> len 0)
      (loop [start (int (dec len))
             to-print-before-nl (int max-len)]
        (let [next-start (int (dec start))
              next-to-print-before-nl (int (dec to-print-before-nl))
              in-c (int (. s charAt start))
              out-c (int (comp in-c))]
          (. bw write out-c)
          (if (zero? next-to-print-before-nl)
            (do
              (. bw newLine)
              (when (not (zero? start))
                (recur next-start max-len)))
            (do
              (when (not (zero? start))
                (recur next-start next-to-print-before-nl))))))
      ;; Need one more newline at the end if the string was not a
      ;; multiple of max-len characters.
      (when (not= 0 (rem len max-len))
        (. bw newLine))
      )))


(defn -main [& args]
  (let [max-dna-chars-per-line 60
        br (java.io.BufferedReader. *in*)
        bw (java.io.BufferedWriter. *out* (* 16 8192))
        ;; We could use the map complement-dna-char-map instead of
        ;; complement-dna-char-fn, but when I tested that, the program
        ;; spent a lot of time running the hashCode method on
        ;; characters.  I'm hoping this is faster.
        complement-dna-char-vec (make-vec-char-mapper complement-dna-char-map)]
    (loop [[desc-str dna-seq-str more] (fasta-slurp-br br)]
      (println-string-to-buffered-writer bw desc-str)
      (print-reverse-complement-of-str-in-lines bw dna-seq-str
                                                complement-dna-char-vec
                                                max-dna-chars-per-line)
      (when more
        (recur (fasta-slurp-br br))))
    (. bw flush))
  (. System (exit 0)))
