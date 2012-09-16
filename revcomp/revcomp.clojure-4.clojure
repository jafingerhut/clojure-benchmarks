;;   The Computer Language Benchmarks Game
;;   http://shootout.alioth.debian.org/

;; contributed by Andy Fingerhut

(ns revcomp
  (:gen-class))

(set! *warn-on-reflection* true)

(gen-class
 :name revcomp.ReversibleByteArray
 :extends java.io.ByteArrayOutputStream
 :exposes {count {:get getCount},
           buf {:get getBuf}}
 :prefix rba-
 :methods [ [ reverse [ "[B" ] void ] ])


(defn find-first-byte-idx [#^bytes buf start search-val]
  (let [search-val (int search-val)]
    (loop [i (int start)]
      (if (== (int (aget buf i)) search-val)
        i
        (recur (unchecked-inc i))))))


(defn reverse-and-complement! [#^bytes buf begin end #^bytes map-char-array nl]
  (let [nl (int nl)]
    (loop [begin (int begin)
           end   (int end)]
      (let [cb (int (aget buf begin))
            ce (int (aget buf end))
            begin (int (if (== cb nl) (unchecked-inc begin) begin))
            end   (int (if (== ce nl) (unchecked-dec end) end))
            cb2 (int (aget buf begin))
            ce2 (int (aget buf end))]
        (when (<= begin end)
          (aset buf begin (aget map-char-array ce2))
          (aset buf end   (aget map-char-array cb2))
          (recur (unchecked-inc begin) (unchecked-dec end)))))))


(defn rba-reverse [#^revcomp.ReversibleByteArray this #^bytes map-char-array]
  (let [count (int (. this (getCount)))
        #^bytes buf (. this (getBuf))
        nl (int \newline)]
    (when (> count 0)
      (let [begin (inc (find-first-byte-idx buf 0 nl))
            end (dec count)]
        (reverse-and-complement! buf begin end map-char-array nl))
      (. System/out write buf 0 count))))


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


(defn ubyte [val]
  (if (>= val 128)
    (byte (- val 256))
    (byte val)))


(defn make-array-char-mapper [cmap]
  (byte-array 256 (map (fn [i]
                         (if (contains? cmap (char i))
                           (ubyte (int (cmap (char i))))
                           (ubyte i)))
                       (range 256))))


(defn -main [& args]
  (let [in System/in
	out System/out
        line (byte-array 82)
        buf (new revcomp.ReversibleByteArray)
        complement-dna-char-array (make-array-char-mapper
				   complement-dna-char-map)
        gt (int \>)]
    (loop [nread (int (. in read line))]
      (when (not= nread (int -1))
        (loop [i (int 0)
               last (int 0)]
          (if (== i nread)
            (. buf write line last (unchecked-subtract nread last))
            (if (== (int (aget line i)) gt)
              (do
                (. buf write line last (unchecked-subtract i last))
                (. buf reverse complement-dna-char-array)
                (. buf reset)
                (recur (unchecked-inc i) i))
              ;; else
              (recur (unchecked-inc i) last))))
        (recur (int (. in read line)))))
    (. buf reverse complement-dna-char-array)))
