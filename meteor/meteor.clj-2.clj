;; The Computer Language Benchmarks Game
;; http://shootout.alioth.debian.org/
;;
;; contributed by Andy Fingerhut
;; Based upon ideas from GCC version by Christian Vosteen (good comments!)

(ns meteor
  (:gen-class)
  (:require [clojure.string :as str])
  (:require [clojure.pprint :as pprint]))

(set! *warn-on-reflection* true)

;(def debug false)
(def debug true)


;; The board is a 50 cell hexagonal pattern.  For    . . . . .
;; maximum speed the board will be implemented as     . . . . .
;; 50 bits, which will fit into a 64-bit long int.   . . . . .
;;                                                    . . . . .
;;                                                   . . . . .
;; I will represent 0's as empty cells and 1's        . . . . .
;; as full cells.                                    . . . . .
;;                                                    . . . . .
;;                                                   . . . . .
;;                                                    . . . . .

;; Here are the numerical indices for each position on the board, also
;; later called board indices.
;;
;;  0   1   2   3   4
;;    5   6   7   8   9
;; 10  11  12  13  14
;;   15  16  17  18  19
;; 20  21  22  23  24
;;   25  26  27  28  29
;; 30  31  32  33  34
;;   35  36  37  38  39
;; 40  41  42  43  44
;;   45  46  47  48  49


;; The puzzle pieces are each specified as a tree.  Every piece
;; consists of 5 'nodes' that occupy one board index.  Each piece has
;; a root node numbered 0, and every other node (numbered 1 through 4)
;; specifies its parent node, and the direction to take to get from
;; the parent to the child (in a default orientation).

;; In the pictures below, pieces are shown graphically in their
;; default orientation, with nodes numbered 0 through 4.

;;   Piece 0   Piece 1   Piece 2   Piece 3   Piece 4
;;                   
;;  0 1 2 3    0   3 4   0 1 2     0 1 2     0   3
;;         4    1 2           3       3       1 2
;;                           4         4         4
;;
(def piece-defs [ [[0 :E ] [1 :E ] [2 :E ] [3 :SE]]  ; piece 0
;;                 ^^^^^^^ node 1 is :E of its parent node 0
;;                         ^^^^^^^ node 2 is :E of its parent node 1
                  [[0 :SE] [1 :E ] [2 :NE] [3 :E ]]  ; piece 1
                  [[0 :E ] [1 :E ] [2 :SE] [3 :SW]]  ; piece 2
                  [[0 :E ] [1 :E ] [2 :SW] [3 :SE]]  ; piece 3
                  [[0 :SE] [1 :E ] [2 :NE] [2 :SE]]  ; piece 4
;;                                          ^ node 4's parent is 2, not 3
;;
;;   Piece 5   Piece 6   Piece 7   Piece 8   Piece 9
;;
;;    0 1 2     0 1       0 1     0 1        0 1 2 3
;;       3 4       2 4       2       2 3 4        4
;;                  3       4 3
;;
                  [[0 :E ] [1 :E ] [2 :SW] [3 :E ]]  ; piece 5
                  [[0 :E ] [1 :SE] [2 :SE] [3 :NE]]  ; piece 6
                  [[0 :E ] [1 :SE] [2 :SE] [3 :W ]]  ; piece 7
                  [[0 :E ] [1 :SE] [2 :E ] [3 :E ]]  ; piece 8
                  [[0 :E ] [1 :E ] [2 :E ] [3 :SW]]  ; piece 9
                  ])

;; Unlike Christian Vosteen's C program, I will only use 6 directions:
;;
;; E SE SW W NW NE
;;
;; I will use a different representation for piece shapes so that I
;; won't need 12 directions for the reason that he introduced them
;; (i.e. pieces whose shapes are like a tree, and cannot be
;; represented only with a sequence of directions from one starting
;; point).


;; To minimize the amount of work done in the recursive solve function
;; below, I'm going to precalculate all legal rotations of each piece
;; at each position on the board. That's 10 pieces x 50 board
;; positions x 6 rotations x 2 'flip positions' ('top side up' or 'top
;; side down').  However, not all 6x2=12 rotations will fit on every
;; cell, so I'll have to keep count of the actual number that do.  The
;; pieces are going to be longs just like the board so they can be
;; bitwise-anded with the board to determine if they fit.  I'm also
;; going to record the next possible open cell for each piece and
;; location to reduce the burden on the solve function.


;; Returns the direction rotated 60 degrees clockwise
(defn rotate [dir]
  (case dir
        :E  :SE
        :SE :SW
        :SW :W
        :W  :NW
        :NW :NE
        :NE :E))

;; Returns the direction flipped on the horizontal axis
(defn flip [dir]
  (case dir
        :E  :E
        :SE :NE
        :SW :NW
        :W  :W
        :NW :SW
        :NE :SE))


;; Returns the new cell index from the specified cell in the specified
;; direction.  The index is only valid if the starting cell and
;; direction have been checked by the out-of-bounds function first.

(defn shift [cell dir]
  (case dir
        :E  (inc cell)
        :SE (if (odd? (quot cell 5))
              (+ cell 6)
              (+ cell 5))
        :SW (if (odd? (quot cell 5))
              (+ cell 5)
              (+ cell 4))
        :W  (dec cell)
        :NW (if (odd? (quot cell 5))
              (- cell 5)
              (- cell 6))
        :NE (if (odd? (quot cell 5))
              (- cell 4)
              (- cell 5))))


;; Returns wether the specified cell and direction will land outside
;; of the board.  Used to determine if a piece is at a legal board
;; location or not.

(defn out-of-bounds [cell dir]
  (case dir
        :E (== (rem cell 5) 4)        ; cell is on the right side
        :SE (or (== (rem cell 10) 9)  ; cell is on "extreme" right side
                (>= cell 45))         ; or the bottom row
        :SW (or (== (rem cell 10) 0)  ; cell is on "extreme" left side
                (>= cell 45))         ; or the bottom row
        :W (== (rem cell 5) 0)        ; cell is on the left side
        :NW (or (== (rem cell 10) 0)  ; cell is on "extreme" left side
                (< cell 5))           ; or the top row
        :NE (or (== (rem cell 10) 9)  ; cell is on "extreme" right side
                (< cell 5))))         ; or the top row


;; Return a piece that is the the same as the one given as argument,
;; except rotated 60 degrees clockwise.

(defn rotate-piece [piece]
  (vec (map (fn [[parent dir]] [parent (rotate dir)]) piece)))


;; Return a piece that is the the same as the one given as argument,
;; except flipped along the horizontal axis.

(defn flip-piece [piece]
  (vec (map (fn [[parent dir]] [parent (flip dir)]) piece)))


;; Convenience function to calculate and return a vector of all of the
;; board indices that a piece's nodes will be in, if that piece's root
;; node is at root-index.

;; Note that no check is made to see whether the piece actually fits
;; on the board or not, so some of the returned index values may be
;; nonsense.  See cells-fit-on-board for a way to check this.

(defn calc-cell-indices [piece root-index]
  (loop [indices [root-index]
         node (int 0)]
    (if (== node 4)
      indices
      ;; else
      ;; Note that information about node n of a piece is in (piece
      ;; (dec n)) We're intentionally iterating the value 'node' 0
      ;; through 3 rather than 1 through 4 here just to avoid
      ;; calculating (dec node) here.
      (let [[parent dir] (piece node)]
        (recur (conj indices (shift (indices parent) dir))
               (inc node))))))


;; Convenience function to calculate if a piece fits on the board.
;; Node 0 of the piece, at board index (indices 0), is assumed to be
;; on the board, but the other nodes may be off.

(defn cells-fit-on-board [piece indices]
  (and
   (let [[parent dir] (piece 0)]  ;; check node 1 of the piece
     (not (out-of-bounds (indices parent) dir)))
   (let [[parent dir] (piece 1)]  ;; node 2, etc.
     (not (out-of-bounds (indices parent) dir)))
   (let [[parent dir] (piece 2)]
     (not (out-of-bounds (indices parent) dir)))
   (let [[parent dir] (piece 3)]
     (not (out-of-bounds (indices parent) dir)))))


;; Fill the entire board going cell by cell, starting from index i.
;; If any cells are "trapped" they will be left alone.

(defn fill-contiguous-space! [^ints board i]
  (let [i (int i)]
    (when (zero? (aget board i))
      (aset board i (int 1))
      (if (not (out-of-bounds i :E))
        (fill-contiguous-space! board (shift i :E)))
      (if (not (out-of-bounds i :SE))
        (fill-contiguous-space! board (shift i :SE)))
      (if (not (out-of-bounds i :SW))
        (fill-contiguous-space! board (shift i :SW)))
      (if (not (out-of-bounds i :W))
        (fill-contiguous-space! board (shift i :W)))
      (if (not (out-of-bounds i :NW))
        (fill-contiguous-space! board (shift i :NW)))
      (if (not (out-of-bounds i :NE))
        (fill-contiguous-space! board (shift i :NE))))))


(defn empty-cells [^ints board-arr]
  (areduce board-arr i c 0 (+ c (if (zero? (aget board-arr i)) 1 0))))


;; Warning: Modifies Java int array board-arr

(defn board-empty-region-sizes! [^ints board-arr]
  (loop [sizes []
         num-empty (empty-cells board-arr)]
    (if (zero? num-empty)
      sizes
      ;; else
      (let [first-empty-cell (loop [i (int 0)]
                               (if (zero? (aget board-arr i))
                                 i
                                 (recur (inc i))))]
        (fill-contiguous-space! board-arr first-empty-cell)
        (let [next-num-empty (empty-cells board-arr)]
          (recur (conj sizes (- num-empty next-num-empty))
                 next-num-empty))))))


;; Generate the long that will later be anded with the board to
;; determine if it fits.

(defn bitmask-from-indices [indices]
  (reduce bit-or (map #(bit-shift-left 1 %) indices)))


(defn print-board [soln]
  (doseq [[line-num v] (map (fn [& args] (vec args)) (range 10) (partition 5 soln))]
    (if (odd? line-num)
      (print " "))
    (println (str/join " " v))))


(defn encode-solution [piece-num-vec mask-vec]
  (let [soln (int-array 50 -1)]
    (loop [i 0]
      (when (< i 50)
        ;; Note: If you use dotimes, the loop variable is cast to an
        ;; int, and this causes bit-shift-left to do a shift by (i %
        ;; 32).
        (let [idx-mask (bit-shift-left 1 i)]
          (loop [p (int 0)]
            (if (< p (count mask-vec))
              (if (zero? (bit-and (mask-vec p) idx-mask))
                (recur (inc p))
                (aset soln i (int (piece-num-vec p)))))))
        (recur (inc i))))
;    (print-board (seq soln))
    (vec
     (map (fn [i] (if (neg? i) "." i))
          (seq soln)))))


;; To thin the number of pieces, I calculate if any of them trap any
;; empty cells at the edges.  There are only a handful of exceptions
;; where the the board can be solved with the trapped cells.  For
;; example: piece 8 can trap 5 cells in the corner, but piece 3 can
;; fit in those cells, or piece 0 can split the board in half where
;; both halves are viable.

(defn one-piece-has-island [indices]
  ;; temp-board and temp-board2 are by default initialized to 0s
  (let [temp-board (int-array 50)]

    ;; Mark the piece board positions as filled in both boards.
    (doseq [idx indices]
      (aset temp-board idx (int 1)))

    (let [empty-region-sizes (board-empty-region-sizes! temp-board)]
(comment
      (when debug
        (when (and (> (count empty-region-sizes) 1)
                   (every? #(zero? (rem % 5)) empty-region-sizes))
          (println "piece partitions board into more than one multiply-of-5 empty region" empty-region-sizes)
          (let [tmp (bitmask-from-indices indices)]
            (println (format "bitmask=0x%x" tmp))
            (print-board (encode-solution [0] [tmp])))))
)
      (not (every? #(zero? (rem % 5)) empty-region-sizes)))))


;; The use of this function in combination with one-piece-has-island
;; produces too few solutions.  It misses many because even though
;; indices from 1 to (min_index-1) of the piece are filled in,
;; typically some later ones are, too, and those can fill in slots
;; that are found as islands by one-piece-has-islands2.

;(defn one-piece-has-island2 [indices]
;  ;; temp-board and temp-board2 are by default initialized to 0s
;  (let [temp-board2 (int-array 50)]
;
;    ;; Mark the piece board positions as filled in both boards.
;    (doseq [idx indices]
;      (aset temp-board2 idx (int 1)))
;
;    ;; Also mark all board positions before the minimum piece index as
;    ;; filled, because during the solve routine, a piece will only be
;    ;; placed at this position after all positions with smaller
;    ;; indexes are completely full.
;    (dotimes [idx (apply min indices)]
;      (aset temp-board2 idx (int 1)))
;
;    (let [empty-region-sizes2 (board-empty-region-sizes! temp-board2)
;          empty-regions-except-largest2 (rest (sort #(compare %2 %1) empty-region-sizes2))]
;      (not (every? #(zero? (rem % 5)) empty-regions-except-largest2)))))



;    ;; Find any empty space on the board, and fill it and any part of
;    ;; the board that can be reached contiguously from there that is
;    ;; not already filled.
;    (loop [i (int 49)]
;      (if (zero? (aget temp-board i))
;        (fill-contiguous-space! temp-board i)
;        (recur (dec i))))
;
;    ;; Count the number of filled spaces.  It will be < 50 if the
;    ;; board was partitioned into separate contiguous regions by the
;    ;; piece.  It will be 50 otherwise.
;    (let [full-count (areduce temp-board i c 0 (+ c (aget temp-board i)))]
;      ;; TBD: The GCC version used a fancier condition than this,
;      ;; which I may use if it helps speed things up significantly.
;      ;; However, those conditions rely upon detailed knowledge of the
;      ;; board and the shape of the pieces.  This condition only
;      ;; relies upon the knowledge that every piece has 5 nodes.
;      (if (zero? (rem full-count 5))
;        false       ; no islands
;        true))))    ; islands


;; Calculate the lowest possible open cell if the piece is placed on
;; the board.  Used to later reduce the amount of time searching for
;; open cells in the solve function.

(defn first-empty-cell-after [minimum indices]
  (let [idx-set (set indices)]
    (loop [i (int minimum)]
      (if (idx-set i)
        (recur (inc i))
        i))))


;; Calculate all six rotations of the specified piece at the specified
;; index.

;; TBD: We calculate only half of piece 3's rotations.  This is
;; because any solution found has an identical solution rotated 180
;; degrees.  Thus we can reduce the number of attempted pieces in the
;; solve algorithm by not including the 180- degree-rotated pieces of
;; ONE of the pieces.  I chose piece 3 because it gave me the best
;; time ;)

;; Note: I've put most of the meat of this code into calc-pieces, so
;; this one isn't needed as it is.

;(defn calc-six-rotations [piece-num piece root-index]
;  (let [rs (take 6 (iterate rotate-piece piece))
;        is (map (fn [piece] [piece (calc-cell-indices piece root-index)]) rs)
;        fitters (filter (fn [[piece indices]]
;                          (and (cells-fit-on-board piece indices)
;                               (not (one-piece-has-island indices))))
;                        is)]
;    (map (fn [[piece indices]]
;           (let [minimum (apply min indices)]
;             {:piece-num piece-num
;              :minimum minimum
;              :next-index (first-empty-cell-after minimum indices)
;              :piece-mask (bitmask-from-indices indices)}))
;         fitters)))


;; The version below might be faster.

;(defn calc-six-rotations [piece-num piece root-index]
;  (loop [rotation (int 0)
;         piece piece
;         indices (calc-cell-indices piece root-index)
;         rotations []]
;    (if (== rotation (int 6))
;      rotations
;      ;; else
;      (let [next-piece (rotate-piece piece)]
;        (recur
;         (inc rotation)
;         next-piece
;         (calc-cell-indices next-piece root-index)
;         (if (and (cells-fit-on-board piece indices)
;                  (tbd))
;           ;; then
;           (let [minimum (apply min indices)]
;             (conj rotations {:piece-num piece-num
;                              :minimum minimum
;                              :next-index (first-empty-cell-after minimum
;                                                                  indices)
;                              :piece-mask (bitmask-from-indices indices)}))
;           ;; else
;           rotations))))))


;; Calculate every legal rotation for each piece at each board
;; location.

(defn calc-pieces [pieces]
  (let [all (apply concat
             (for [p (range (count pieces))
                   index (range 0 50)]
               (map (fn [piece] [p piece index])
                    (concat
                     (take 6 (iterate rotate-piece (pieces p)))  ; top side up
                     (take 6 (iterate rotate-piece (flip-piece (pieces p)))))))) ; flipped
;        junk (when debug (println (format "(class all)=%s" (class all))))
;        junk (when debug
;               (println (format "%d position/flip/rotation possibilities" (count all))))
        with-indices (map (fn [[piece-num piece root-index]]
                            [piece-num piece
                             (calc-cell-indices piece root-index)])
                          all)
;        junk (when debug
;               (doseq [[piece-num piece indices] with-indices]
;                 (when (and (cells-fit-on-board piece indices)
;                            (not (one-piece-has-island indices))
;                            (one-piece-has-island2 indices))
;                   (println "This piece has no islands by itself, but does when earlier board indices are full:")
;                   (print-board (encode-solution [piece-num]
;                                                 [(bitmask-from-indices indices)])))))
        keepers (filter (fn [[piece-num piece indices]]
                          (and (cells-fit-on-board piece indices)
                               (not (one-piece-has-island indices))))
                        with-indices)
;        junk (when debug
;               (println (format "%d keepers that fit, without islands" (count keepers))))
        processed (map (fn [[piece-num piece indices]]
                         (let [minimum (apply min indices)]
                           {:piece-num piece-num
                            :minimum minimum
                            :next-index (first-empty-cell-after minimum indices)
                            :piece-mask (bitmask-from-indices indices)}))
                       keepers)]
    ;; Create a table indexed by [piece number, minimum index occupied
    ;; by the piece], where each table entry contains a vector of the
    ;; following sets of information (stored in maps):
    ;;
    ;; (1) the bit mask of board indices occupied by the piece in a
    ;;     particular flip/rotation, and
    ;;
    ;; (2) the smallest index that is not filled by any part of the
    ;;     piece, that is larger than the piece's minimum occupied
    ;;     index.
    (reduce (fn [tbl p]
              (let [k [(:piece-num p) (:minimum p)]]
                (assoc tbl k
                       (conj (if (contains? tbl k)
                               (tbl k)
                               [])
                             {:piece-mask (:piece-mask p)
                              :next-index (:next-index p)}))))
            {}
            processed)))


;(defn first-empty-index [idx board]
;  (loop [i (int idx)]
;    (if (zero? (bit-and board (bit-shift-left (long 1) i)))
;      i
;      (recur (inc i)))))


(defn first-empty-index [idx board]
  (loop [i idx
         board (bit-shift-right board i)]
    (if (zero? (bit-and board (int 1)))
      i
      (recur (inc i) (bit-shift-right board 1)))))


(defn print-board-full-empty-only [board]
  (printf "board=0x%04x\n" board)
  (doseq [[line-num v] (map (fn [& args] (vec args))
                            (range 10)
                            (partition 5
                                       (map #(if (zero? (bit-and 1 (bit-shift-right board
                                                                                    %)))
                                               "."
                                               1)
                                            (range 50))))]
    (if (odd? line-num)
      (print " "))
    (println (str/join " " v))))


(defn create-triples []
  (let [bad-even-triples (int-array (/ (bit-shift-left 1 15) 32))
        bad-odd-triples (int-array (/ (bit-shift-left 1 15) 32))
        temp-arr (int-array 50)]
(comment
    (when debug (println "Creating bad-even-triples..."))
)
    (dotimes [row0 32]
      (dotimes [row1 32]
        (dotimes [row2 32]
          (let [board (bit-or (bit-or row0 (bit-shift-left row1 5))
                              (bit-shift-left row2 10))]
            (dotimes [i 15]
              (aset temp-arr i (int (bit-and 1 (bit-shift-right board i)))))
            (dotimes [i 35]
              (aset temp-arr (+ 15 i) (int 0)))
;            (print "Checking this board for islands:")
;            (printf "board=0x%04x " board)
;            (println (str/join (map #(aget temp-arr %) (range 15))))
            (let [empty-region-sizes (board-empty-region-sizes! temp-arr)
                  empty-sizes-except-largest (rest (sort #(compare %2 %1) empty-region-sizes))
                  j (int (bit-shift-right board 5))
                  i (int (rem board 32))]
;              (println "Checking this board for islands:" empty-region-sizes)
;              (print-board-full-empty-only board)
              (when-not (every? #(zero? (rem % 5)) empty-sizes-except-largest)
                ;; then it is possible for pieces to fill the empty
                ;; regions
;                (println "This even board has islands:" empty-sizes-except-largest)
;                (print-board-full-empty-only board)
                (aset bad-even-triples j
                      (bit-or (aget bad-even-triples j)
                              (bit-shift-left (int 1) i)))
                ;; else it is impossible for pieces that fill
                ;; 5 board indices to fill all of the
                ;; empty regions.
                ))))))
(comment
    (when debug
      (println "Done.")
      (println "Creating bad-odd-triples..."))
)
    (dotimes [i 5]
      (aset temp-arr i (int 1)))
    (dotimes [row1 32]
      (dotimes [row2 32]
        (dotimes [row3 32]
          (let [board-rows-1-3 (bit-or (bit-or row1 (bit-shift-left row2 5))
                                       (bit-shift-left row3 10))
                board (bit-or 0x1F (bit-shift-left board-rows-1-3 5))]
            (dotimes [i 15]
              (aset temp-arr (+ 5 i) (int (bit-and 1 (bit-shift-right board-rows-1-3 i)))))
            (dotimes [i 30]
              (aset temp-arr (+ 20 i) (int 0)))
            (let [empty-region-sizes (board-empty-region-sizes! temp-arr)
                  empty-sizes-except-largest (rest (sort #(compare %2 %1)
                                                         empty-region-sizes))
                  j (int (bit-shift-right board-rows-1-3 5))
                  i (int (rem board-rows-1-3 32))]
              (when-not (every? #(zero? (rem % 5)) empty-sizes-except-largest)
;                (println "This odd board has islands:")
;                (print-board-full-empty-only board)
                (aset bad-odd-triples j
                      (bit-or (aget bad-odd-triples j)
                              (bit-shift-left 1 i)))
                ))))))
(comment
    (when debug (println "Done."))
)
    [bad-even-triples bad-odd-triples]))


(defn create-quadruples []
  (let [bad-even-quadruples (int-array (/ (bit-shift-left 1 20) 32))
        bad-odd-quadruples (int-array (/ (bit-shift-left 1 20) 32))
        temp-arr (int-array 50)]
    (when debug (println "Creating bad-even-quadruples..."))
    (dotimes [row0 32]
      (dotimes [row1 32]
        (dotimes [row2 32]
          (dotimes [row3 32]
          (let [board (bit-or (bit-or row0 (bit-shift-left row1 5))
                              (bit-or (bit-shift-left row2 10) (bit-shift-left row3 15)))]
            (dotimes [i 20]
              (aset temp-arr i (int (bit-and 1 (bit-shift-right board i)))))
            (dotimes [i 30]
              (aset temp-arr (+ 12 i) (int 0)))
;            (print "Checking this board for islands:")
;            (printf "board=0x%04x " board)
;            (println (str/join (map #(aget temp-arr %) (range 15))))
            (let [empty-region-sizes (board-empty-region-sizes! temp-arr)
                  empty-sizes-except-largest (rest (sort #(compare %2 %1) empty-region-sizes))
                  j (int (bit-shift-right board 5))
                  i (int (rem board 32))]
;              (println "Checking this board for islands:" empty-region-sizes)
;              (print-board-full-empty-only board)
              (when-not (every? #(zero? (rem % 5)) empty-sizes-except-largest)
                ;; then it is possible for pieces to fill the empty
                ;; regions
;                (println "This even board has islands:" empty-sizes-except-largest)
;                (print-board-full-empty-only board)
                (aset bad-even-quadruples j
                      (bit-or (aget bad-even-quadruples j)
                              (bit-shift-left (int 1) i)))
                ;; else it is impossible for pieces that fill
                ;; 5 board indices to fill all of the
                ;; empty regions.
                )))))))
    (when debug
      (println "Done.")
      (println "Creating bad-odd-quadruples..."))
    (dotimes [i 5]
      (aset temp-arr i (int 1)))
    (dotimes [row1 32]
      (dotimes [row2 32]
        (dotimes [row3 32]
          (dotimes [row4 32]
          (let [board-rows-1-4 (bit-or (bit-or row1 (bit-shift-left row2 5))
                                       (bit-or (bit-shift-left row3 10) (bit-shift-left row4 15)))
                board (bit-or 0x1F (bit-shift-left board-rows-1-4 5))]
            (dotimes [i 20]
              (aset temp-arr (+ 5 i) (int (bit-and 1 (bit-shift-right board-rows-1-4 i)))))
            (dotimes [i 25]
              (aset temp-arr (+ 25 i) (int 0)))
            (let [empty-region-sizes (board-empty-region-sizes! temp-arr)
                  empty-sizes-except-largest (rest (sort #(compare %2 %1)
                                                         empty-region-sizes))
                  j (int (bit-shift-right board-rows-1-4 5))
                  i (int (rem board-rows-1-4 32))]
              (when-not (every? #(zero? (rem % 5)) empty-sizes-except-largest)
;                (println "This odd board has islands:")
;                (print-board-full-empty-only board)
                (aset bad-odd-quadruples j
                      (bit-or (aget bad-odd-quadruples j)
                              (bit-shift-left 1 i)))
                )))))))
    (when debug (println "Done."))
    [bad-even-quadruples bad-odd-quadruples]))


(def num-solutions (int-array 1))
(def all-solutions (object-array 2200))

(defn record-solution! [soln]
  (let [^ints num-solutions num-solutions
        ^objects all-solutions all-solutions
        n (int (aget num-solutions (int 0)))]
    (aset all-solutions n soln)
    (aset num-solutions (int 0) (inc n))))


(comment
(defmacro my-shift-long-right [val shft]
  `(let [val# (long ~val)
        shft# (int ~shft)]
    (case shft#
      0 (bit-shift-right val# (int  0))
      1 (bit-shift-right val# (int  1))
      2 (bit-shift-right val# (int  2))
      3 (bit-shift-right val# (int  3))
      4 (bit-shift-right val# (int  4))
      5 (bit-shift-right val# (int  5))
      6 (bit-shift-right val# (int  6))
      7 (bit-shift-right val# (int  7))
      8 (bit-shift-right val# (int  8))
      9 (bit-shift-right val# (int  9))
     10 (bit-shift-right val# (int 10))
     11 (bit-shift-right val# (int 11))
     12 (bit-shift-right val# (int 12))
     13 (bit-shift-right val# (int 13))
     14 (bit-shift-right val# (int 14))
     15 (bit-shift-right val# (int 15))
     16 (bit-shift-right val# (int 16))
     17 (bit-shift-right val# (int 17))
     18 (bit-shift-right val# (int 18))
     19 (bit-shift-right val# (int 19))
     20 (bit-shift-right val# (int 20))
     21 (bit-shift-right val# (int 21))
     22 (bit-shift-right val# (int 22))
     23 (bit-shift-right val# (int 23))
     24 (bit-shift-right val# (int 24))
     25 (bit-shift-right val# (int 25))
     26 (bit-shift-right val# (int 26))
     27 (bit-shift-right val# (int 27))
     28 (bit-shift-right val# (int 28))
     29 (bit-shift-right val# (int 29))
     30 (bit-shift-right val# (int 30))
     31 (bit-shift-right val# (int 31))
     32 (bit-shift-right val# (int 32))
     33 (bit-shift-right val# (int 33))
     34 (bit-shift-right val# (int 34))
     35 (bit-shift-right val# (int 35))
     36 (bit-shift-right val# (int 36))
     37 (bit-shift-right val# (int 37))
     38 (bit-shift-right val# (int 38))
     39 (bit-shift-right val# (int 39))
     40 (bit-shift-right val# (int 40))
     41 (bit-shift-right val# (int 41))
     42 (bit-shift-right val# (int 42))
     43 (bit-shift-right val# (int 43))
     44 (bit-shift-right val# (int 44))
     45 (bit-shift-right val# (int 45))
     46 (bit-shift-right val# (int 46))
     47 (bit-shift-right val# (int 47))
     48 (bit-shift-right val# (int 48))
     49 (bit-shift-right val# (int 49)))))
)


;; depth is 0 on the first call, and is 1 more for each level of
;; nested recursive call.  It is equal to the number of pieces placed
;; on the board in the partial solution so far.

;; board is a long representing which board cells are occupied (bit
;; value 1) or empty (bit value 0), based upon the pieces placed so
;; far.

;; cell is the board index in [0,49] that should be checked first to
;; see if it is empty.

;; piece-num-vec is a vector of the piece-nums placed so far, in the
;; order they were placed, i.e. depth order.  (piece-vec 0) was placed
;; at depth 0, etc.  [] in root call.  (named sol_nums in GCC program)

;; mask-vec is a vector of the bitmasks of the pieces placed so far,
;; in the order they were placed.  [] in root call.  (named sol_masks
;; in GCC program)

(defn solve! [tbl ^ints bad-even-triples ^ints bad-odd-triples
;              ^ints bad-even-quadruples ^ints bad-odd-quadruples
              ]
  (letfn
      [(board-has-islands [board index]
         (cond
          (>= index 40)
          false
          ;(>= index 35)
          :else
          (let [;board (long board)
                row-num (int (quot index 5))
                row-begin-idx (* 5 row-num)
                current-three-rows (bit-and (bit-shift-right board row-begin-idx)
                                            0x7FFF)
                int-num (int (bit-shift-right current-three-rows 5))
                bit-num (int (rem current-three-rows 32))
                even-row (zero? (rem row-num 2))]
            (if even-row
              (not (zero? (bit-and 1 (bit-shift-right
                                      (aget bad-even-triples int-num) bit-num))))
              (not (zero? (bit-and 1 (bit-shift-right
                                      (aget bad-odd-triples int-num) bit-num))))))
;          :else
;          (let [board (long board)
;                row-num (int (quot index 5))
;                row-begin-idx (int (* 5 row-num))
;                current-four-rows (bit-and (bit-shift-right board row-begin-idx)
;                                            0xFFFFF)
;                int-num (int (bit-shift-right current-four-rows 5))
;                bit-num (int (rem current-four-rows 32))
;                even-row (zero? (rem row-num 2))]
;            (if even-row
;              (not (zero? (bit-and 1 (bit-shift-right
;                                      (aget bad-even-quadruples int-num) bit-num))))
;              (not (zero? (bit-and 1 (bit-shift-right
;                                      (aget bad-odd-quadruples int-num) bit-num))))))
          ))
       (solve-helper [depth board cell unplaced-piece-num-set
                      piece-num-vec mask-vec]
(comment
         (when debug
           (if (= depth 3)
             (println (format "depth %d piece-num-vec=%s" depth piece-num-vec))
           ;(print-board (encode-solution piece-num-vec mask-vec))
             ))
)
         (let [cell (int (first-empty-index cell board))]
           (doseq [piece-num (seq unplaced-piece-num-set)]
             (doseq [{piece-mask :piece-mask, next-index :next-index}
                     (get tbl [piece-num cell])]
               (when (zero? (bit-and board piece-mask))
                 (if (== depth 9)
                   ;; Solution found!
                   (let [sol1 (encode-solution (conj piece-num-vec piece-num)
                                               (conj mask-vec piece-mask))]
                     ;; TBD: After I pick one piece to only use 3
                     ;; rotations of, replace the following line
                     ;; with: [sol1 (reverse sol1)].  (reverse sol1)
                     ;; is sol1 rotated 180 degrees.
;                      (print-board sol1)
                     (record-solution! sol1))
                    ;; else
                   (let [next-board (bit-or board piece-mask)]
                     (when-not (board-has-islands next-board next-index)
                       (solve-helper
                        (inc depth)
                        (bit-or board piece-mask)
                        next-index
                        (disj unplaced-piece-num-set piece-num)
                        (conj piece-num-vec piece-num)
                        (conj mask-vec piece-mask))))))))))]
    (solve-helper 0 (long 0) 0 (set (range 10)) [] [])))


(defn -main [& args]
  (let [tbl (calc-pieces piece-defs)
;        junk (when debug (println (count (keys tbl)) "entries in piece lookup table"))
        tbl2 (reduce (fn [tbl2 [piece-num idx]]
                       (assoc tbl2 idx
                              (+ (count (get tbl [piece-num idx]))
                                 (get tbl2 idx 0))))
                     {}
                     (keys tbl))
        [bad-even-triples bad-odd-triples] (create-triples)
;        [bad-even-quadruples bad-odd-quadruples] (create-quadruples)
        ]
(comment
    (when debug
      (doseq [idx (sort (keys tbl2))]
        (println (format "%d: %d piece-orientations" idx (tbl2 idx)))))
)
    (solve! tbl bad-even-triples bad-odd-triples
;            bad-even-quadruples bad-odd-quadruples
            )
;    (when debug (pprint/pprint (seq all-solutions)))
    (let [^ints num-solutions num-solutions
          n (int (aget num-solutions (int 0)))
          sorted-solns (sort (take n (seq all-solutions)))]
      (println (format "%d solutions found" n))
      (println)
      (print-board (first sorted-solns))
      (println)
      (print-board (nth sorted-solns (dec n))))))
