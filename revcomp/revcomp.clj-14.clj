;   Copyright (c) Rich Hickey and contributors.
;   All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns revcomp
  (:gen-class))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

(def from "ACBDGHK\nMNSRUTWVYacbdghkmnsrutwvy")
(def to "TGVHCDM\nKNSYAAWBRTGVHCDMKNSYAAWBR")
(def compl
  "Byte-array based lookup for complement"
  (reduce
   (fn [^bytes m [from to]]
     (aset m (byte (int from)) (byte (int to)))
     m)
   (byte-array 256)
   (map vector from to)))

(defn revcomp
  "Reverse and complement a in range begin (inclusive) - end (exclusive).
   Updates in place."
  [^bytes a begin end]
  (when-not (= (int \>) (aget a begin))
    (loop [begin begin
           end (dec end)]
      (let [bb (aget a begin)
            be (aget a end)]
        (cond
         (= 10 bb) (recur (inc begin) end)
         (= 10 be) (recur begin (inc end))
         (<= begin end) (do
                          (aset a begin (aget ^bytes compl be))
                          (aset a end (aget ^bytes compl bb))
                          (recur (inc begin) (dec end))))))))

(defn with-each-line-rc
  "Calls revcomp for each line in buf"
  [^bytes buf]
  (let [ct (count buf)]
    (loop [i 0 j 0]
      (if (<= ct j)
        (when (> j (inc i))
          (revcomp buf i j))
        (if (= 10 (aget ^bytes buf j))
          (do
            (when (> j (inc i))
              (revcomp buf i j)
              (recur (inc j) (inc j))))
          (recur i (inc j)))))))

(defn -main [& args]
  (let [buf (byte-array (.available System/in))]
    (.read System/in buf)
    (with-each-line-rc buf)
    (.write System/out buf)))


