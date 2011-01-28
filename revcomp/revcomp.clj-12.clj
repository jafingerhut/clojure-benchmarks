;;   The Computer Language Benchmarks Game
;;   http://shootout.alioth.debian.org/

;; contributed by Andy Fingerhut

(ns revcomp
  (:gen-class))

(set! *warn-on-reflection* true)


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


(defn find-next-nl-idx [idx #^bytes buf nl]
  (loop [idx (int idx)]
    (if (= (aget buf idx) nl)
      idx
      (recur (unchecked-inc idx)))))


(defn find-next-gt-idx [idx #^bytes buf gt len]
  (let [gt (int gt)
        len (int len)]
    (loop [idx (int idx)]
      (if (or (== idx len)
              (== (int (aget buf idx)) gt))
        idx
        (recur (unchecked-inc idx))))))


(defn -main [& args]
  (let [in-size (int (.available System/in))
        buf (byte-array in-size)
        complement-dna-char-array (make-array-char-mapper
				   complement-dna-char-map)]
    (.read System/in buf)
    (let [len (int (alength buf))
          nl (byte (int \newline))
          gt (byte (int \>))]
      (loop [i (int 0)]
        (when (not= i len)
          (let [next-nl-idx (int (find-next-nl-idx i buf nl))
                next-gt-idx (int (find-next-gt-idx next-nl-idx buf gt len))]
            (reverse-and-complement! buf next-nl-idx
                                     (unchecked-subtract next-gt-idx 2)
                                     complement-dna-char-array nl)
            (recur next-gt-idx)))))
    (.write System/out buf)))
