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


;; The board is a 50 cell hexagonal pattern.  For    . . . . .
;; maximum speed the board will be implemented as     . . . . .
;; 50 bits, which will fit into 2 32-bit ints.       . . . . .
;; I originally tried 1 64-bit long, but the bit-*    . . . . .
;; operators in Clojure 1.2 are not as optimized     . . . . .
;; as they will be in the next version of Clojure.    . . . . .
;;                                                   . . . . .
;;                                                    . . . . .
;; I will represent 0's as empty cells and 1's       . . . . .
;; as full cells.                                     . . . . .

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


;; Numerical encodings of directions:
;; 0 East, 1 Southeast, 2 Southwest, 3 West, 4 Northwest, 5 Northeast

;; Each puzzle piece is specified as a tree.  Every piece consists of
;; 5 'nodes', each of which occupies one board index.  Each piece has
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
(def piece-defs [ [[0 0] [1 0] [2 0] [3 1]]  ; piece 0
;;                 ^^^^^ node 1 is East (direction 0) of its parent node 0
;;                       ^^^^^ node 2 is East of its parent node 1
                  [[0 1] [1 0] [2 5] [3 0]]  ; piece 1
                  [[0 0] [1 0] [2 1] [3 2]]  ; piece 2
                  [[0 0] [1 0] [2 2] [3 1]]  ; piece 3
                  [[0 1] [1 0] [2 5] [2 1]]  ; piece 4
;;                                    ^ node 4's parent is 2, not 3
;;
;;   Piece 5   Piece 6   Piece 7   Piece 8   Piece 9
;;
;;    0 1 2     0 1       0 1     0 1        0 1 2 3
;;       3 4       2 4       2       2 3 4        4
;;                  3       4 3
;;
                  [[0 0] [1 0] [2 2] [3 0]]  ; piece 5
                  [[0 0] [1 1] [2 1] [3 5]]  ; piece 6
                  [[0 0] [1 1] [2 1] [3 3]]  ; piece 7
                  [[0 0] [1 1] [2 0] [3 0]]  ; piece 8
                  [[0 0] [1 0] [2 0] [3 2]]  ; piece 9
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
;; side down').  However, not all 6x2=12 orientations will fit on
;; every cell.  Only keep the ones that do.  The pieces are going to
;; be pairs of 32-bit ints just like the board so they can be
;; bitwise-anded with the board to determine if they fit.  I'm also
;; going to record the next possible open cell for each piece and
;; location to reduce the burden on the solve function.


;; Returns the direction rotated 60 degrees clockwise
(defn rotate [dir]
  (case dir
        0 1
        1 2
        2 3
        3 4
        4 5
        5 0))

;; Returns the direction flipped on the horizontal axis
(defn flip [dir]
  (case dir
        0 0
        1 5
        2 4
        3 3
        4 2
        5 1))


;; Returns the new cell index from the specified cell in the specified
;; direction.  The index is only valid if the starting cell and
;; direction have been checked by the out-of-bounds function first.

(defn shift [cell dir]
  (case dir
        0 (inc cell)
        1 (if (odd? (quot cell 5))
              (+ cell 6)
              (+ cell 5))
        2 (if (odd? (quot cell 5))
              (+ cell 5)
              (+ cell 4))
        3  (dec cell)
        4 (if (odd? (quot cell 5))
              (- cell 5)
              (- cell 6))
        5 (if (odd? (quot cell 5))
              (- cell 4)
              (- cell 5))))


(defn make-shift-table []
  (object-array (map (fn [cell-idx]
                       (int-array (map (fn [dir] (shift cell-idx dir))
                                       (range 6))))
                     (range 50))))


;; Returns wether the specified cell and direction will land outside
;; of the board.  Used to determine if a piece is at a legal board
;; location or not.

(defn out-of-bounds [cell dir]
  (case dir
        0 (== (rem cell 5) 4)       ; cell is on the right side
        1 (or (== (rem cell 10) 9)  ; cell is on "extreme" right side
              (>= cell 45))         ; or the bottom row
        2 (or (== (rem cell 10) 0)  ; cell is on "extreme" left side
              (>= cell 45))         ; or the bottom row
        3 (== (rem cell 5) 0)       ; cell is on the left side
        4 (or (== (rem cell 10) 0)  ; cell is on "extreme" left side
              (< cell 5))           ; or the top row
        5 (or (== (rem cell 10) 9)  ; cell is on "extreme" right side
              (< cell 5))))         ; or the top row


(defn make-oob-table []
  (object-array (map (fn [cell-idx]
                       (boolean-array (map (fn [dir] (out-of-bounds cell-idx dir))
                                           (range 6))))
                     (range 50))))


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

(defn calc-cell-indices [piece root-index ^objects shift-table]
  (loop [indices (transient [root-index])
         node (int 0)]
    (if (== node 4)
      (persistent! indices)
      ;; else
      ;; Note that information about node n of a piece is in (piece
      ;; (dec n)) We're intentionally iterating the value 'node' 0
      ;; through 3 rather than 1 through 4 here just to avoid
      ;; calculating (dec node) here.
      (let [pair (piece node)
            parent (int (pair 0))
            dir (int (pair 1))
            ;[parent dir] (piece node)
            parent-loc (int (indices parent))]
        (recur (conj! indices
                      (if (and (< parent-loc 50) (not (neg? parent-loc)))
                        (let [^ints shift-table-for-parent-loc
                              (aget shift-table parent-loc)]
                          (aget shift-table-for-parent-loc dir))
                        0))  ;; dummy value
               (inc node))))))


;; Convenience function to calculate if a piece fits on the board.
;; Node 0 of the piece, at board index (indices 0), is assumed to be
;; on the board, but the other nodes may be off.

(defmacro node-fits [node-info indices ^objects oob-table]
  `(let [pair# ~node-info
         parent-node-num# (int (pair# 0))
         dir# (int (pair# 1))
         parent-idx# (int (~indices parent-node-num#))
         ;^booleans oob-for-parent-idx# (aget ~oob-table parent-idx#)]
         ^"[Z" oob-for-parent-idx# (aget ~oob-table parent-idx#)]
     (not (aget oob-for-parent-idx# dir#))))


(defn cells-fit-on-board [piece indices ^objects oob-table]
  (and
   (node-fits (piece 0) indices oob-table)  ;; check node 1 of the piece
   (node-fits (piece 1) indices oob-table)  ;; node 2, etc.
   (node-fits (piece 2) indices oob-table)
   (node-fits (piece 3) indices oob-table)))


;; Fill the entire board going cell by cell, starting from index i.
;; If any cells are "trapped" they will be left alone.

(defn fill-contiguous-space! [^ints board i
                              ^objects shift-table ^objects oob-table]
  (letfn
      [(fill-helper! [i]
         (let [i (int i)
               ^booleans oob-table-row (aget oob-table i)
               ^ints shift-table-row (aget shift-table i)]
           (when (zero? (aget board i))
             (aset board i (int 1))
             (if (not (aget oob-table-row (int 0)))
               (fill-helper! (aget shift-table-row (int 0))))
             (if (not (aget oob-table-row (int 1)))
               (fill-helper! (aget shift-table-row (int 1))))
             (if (not (aget oob-table-row (int 2)))
               (fill-helper! (aget shift-table-row (int 2))))
             (if (not (aget oob-table-row (int 3)))
               (fill-helper! (aget shift-table-row (int 3))))
             (if (not (aget oob-table-row (int 4)))
               (fill-helper! (aget shift-table-row (int 4))))
             (if (not (aget oob-table-row (int 5)))
               (fill-helper! (aget shift-table-row (int 5)))))))]
    (fill-helper! i)))


(defn empty-cells [^ints board-arr]
  (- 50
     (let [^ints a board-arr]
       (loop [i (int 49)
              ret (int 0)]
         (if (neg? i)
           ret
           (recur (unchecked-dec i) (unchecked-add ret (aget a i))))))))


;; Warning: Modifies its argument board-arr

(defn board-empty-region-sizes! [^ints board-arr
                                 ^objects shift-table ^objects oob-table]
  (loop [sizes (transient [])
         num-empty (empty-cells board-arr)
         last-empty-cell 50]
    (if (zero? num-empty)
      (persistent! sizes)
      ;; else
      (let [next-last-empty-cell (loop [i (int (dec last-empty-cell))]
                                   (if (zero? (aget board-arr i))
                                     i
                                     (recur (dec i))))]
        (fill-contiguous-space! board-arr next-last-empty-cell shift-table
                                oob-table)
        (let [next-num-empty (empty-cells board-arr)]
          (recur (conj! sizes (- num-empty next-num-empty))
                 next-num-empty
                 next-last-empty-cell))))))


;; Generate the pair of 32-bit ints that will later be anded with the
;; board to determine if it fits.

(defn bitmask-from-indices [indices]
  [(reduce bit-or (map (fn [i] (if (< i 25) (bit-shift-left 1 i) 0))
                       indices))
   (reduce bit-or (map (fn [i] (if (< i 25) 0 (bit-shift-left 1 (- i 25))))
                       indices))])


(defn print-board [^ints soln]
  (dotimes [i 50]
    (when (zero? (unchecked-remainder i 5))
      (println ""))
    (when (== (unchecked-remainder i 10) (int 5))
      (print " "))
    (printf "%d " (aget soln i))))


;; Solutions are encoded as vectors of 50 integers, one for each board
;; index, where each integer is in the range [0,9], representing one
;; of the 5 parts of a piece that is in that board index.

(defn encode-solution [^ints piece-num-arr ^ints mask-arr0 ^ints mask-arr1]
  (let [soln (int-array 50 -1)]
    (dotimes [i 25]
      (let [idx-mask (int (bit-shift-left (int 1) i))]
        (loop [p (int 0)]
          (if (< p (int 10))
            (if (zero? (bit-and (aget mask-arr0 p) idx-mask))
              (recur (inc p))
              (aset soln i (aget piece-num-arr p)))))
        (loop [p (int 0)]
          (if (< p (int 10))
            (if (zero? (bit-and (aget mask-arr1 p) idx-mask))
              (recur (inc p))
              (aset soln (unchecked-add (int 25) i) (aget piece-num-arr p)))))))
    soln))


;; To thin the number of pieces, I calculate if any of them trap any
;; empty cells at the edges, such that the number of trapped empty
;; cells is not a multiple of 5.  All pieces have 5 cells, so any such
;; trapped regions cannot possibly be filled with any pieces.

(defn one-piece-has-island [indices shift-table oob-table]
  (let [temp-board (int-array 50)]
    ;; Mark the piece board positions as filled
    (doseq [idx indices]
      (aset temp-board idx (int 1)))
    (let [empty-region-sizes (board-empty-region-sizes! temp-board shift-table
                                                        oob-table)]
      (not (every? #(zero? (rem % 5)) empty-region-sizes)))))


;; Calculate the lowest possible open cell if the piece is placed on
;; the board.  Used to later reduce the amount of time searching for
;; open cells in the solve function.

(defn first-empty-cell-after [minimum indices]
  (let [idx-set (set indices)]
    (loop [i (int minimum)]
      (if (idx-set i)
        (recur (inc i))
        i))))


;; We calculate only half of piece 3's rotations.  This is because any
;; solution found has an identical solution rotated 180 degrees.  Thus
;; we can reduce the number of attempted pieces in the solve algorithm
;; by not including the 180- degree-rotated pieces of ONE of the
;; pieces.  I chose piece 3 because it gave me the best time ;)

(def *piece-num-to-do-only-3-rotations* 3)

;; Calculate every legal rotation for each piece at each board
;; location.

(defn calc-pieces [pieces shift-table oob-table]
  (let [npieces (int (count pieces))
        ^objects tbl (object-array npieces)] ; first index is piece-num
    (dotimes [piece-num npieces]
      (aset tbl piece-num (object-array 50))
      (let [^objects piece-arr (aget tbl piece-num)]
        (dotimes [cell 50]  ; second index is board index
          ;; Start with transient vectors.  Later we will change them to
          ;; Java arrays after we know how long to make them.
          (aset piece-arr cell (transient [])))))
    ;; Find all possible good piece placements
    (dotimes [p npieces]
      (let [unrotated-piece (pieces p)
            num-rots (if (= p *piece-num-to-do-only-3-rotations*) 3 6)]
        (dotimes [flip 2]
          (loop [rot (int 0)
                 piece (if (zero? flip)
                         unrotated-piece
                         (flip-piece unrotated-piece))]
            (when (< rot num-rots)
              (dotimes [cell 50]
                (let [indices (calc-cell-indices piece cell shift-table)]
                  (when (and (cells-fit-on-board piece indices oob-table)
                             (not (one-piece-has-island indices shift-table
                                                        oob-table)))
                    (let [minimum (apply min indices)
                          [piece-mask0 piece-mask1] (bitmask-from-indices
                                                     indices)
                          next-index (int (first-empty-cell-after minimum
                                                                  indices))]

                      (let [^ints good-placement (int-array 3)
                            ^objects piece-arr (aget tbl p)]
                        (aset good-placement (int 0) (int piece-mask0))
                        (aset good-placement (int 1) (int piece-mask1))
                        (aset good-placement (int 2) next-index)
                        ;; Put it in the table
                        (aset piece-arr minimum
                              (conj! (aget piece-arr minimum) good-placement))
                        )))))
              (recur (inc rot) (rotate-piece piece)))))))
    ;; Make all transient vectors into Java object arrays
    (dotimes [piece-num npieces]
      (let [^objects piece-arr (aget tbl piece-num)]
        (dotimes [cell 50]
          (let [cur-vec (persistent! (aget piece-arr cell))]
            (aset piece-arr cell (object-array cur-vec))))))
    tbl))



;; first-empty-index-aux assumptions: idx is in the range [0,24].
;; half-board is an integer that has bits 25 and higher equal to 0, so
;; the loop is guaranteed to terminate, and the return value will be
;; in the range [0,25].

(defmacro first-empty-index-aux [idx half-board]
  `(loop [i# (int ~idx)
          hb# (int (bit-shift-right ~half-board ~idx))]
     (if (zero? (bit-and hb# (int 1)))
       i#
       (recur (inc i#) (bit-shift-right hb# (int 1))))))


(defmacro first-empty-index [idx board0 board1]
  `(if (< ~idx (int 25))
     (let [i# (int (first-empty-index-aux ~idx ~board0))]
       (if (== i# (int 25))
         (unchecked-add (int 25) (int (first-empty-index-aux (int 0) ~board1)))
         i#))
     (unchecked-add (int 25)
                    (int (first-empty-index-aux (unchecked-subtract ~idx
                                                                    (int 25))
                                                ~board1)))))


;; Note: board-empty-region-sizes! runs faster if there are fewer
;; empty cells to fill.  So fill as much of the board as we can before
;; putting in the 3 partially filled rows.  There must be at least one
;; completely empty row at the bottom in order to correctly determine
;; whether these 3 rows are a bad triple.

(defn create-triples [shift-table oob-table]
  (let [bad-even-triples (int-array (/ (bit-shift-left 1 15) 32))
        bad-odd-triples (int-array (/ (bit-shift-left 1 15) 32))
        temp-arr (int-array 50)]
    ;; Fill rows 0..5 completely.
    (dotimes [i 30]
      (aset temp-arr i (int 1)))
    (dotimes [row6 32]
      (dotimes [row7 32]
        (dotimes [row8 32]
          (let [board (int (bit-or (bit-or row6 (bit-shift-left row7 5))
                                   (bit-shift-left row8 10)))]
            (dotimes [i 15]
              (aset temp-arr (+ (int 30) i)
                    (int (bit-and (int 1) (bit-shift-right board i)))))
            (dotimes [i 5]   ;; Row 9 is completely empty to start with
              (aset temp-arr (+ (int 45) i) (int 0)))
            (let [empty-region-sizes (board-empty-region-sizes!
                                      temp-arr shift-table oob-table)
                  ;; Note that we assume board-empty-region-sizes!
                  ;; returns a sequence, where the first element is
                  ;; the size of the empty region that includes the
                  ;; last cell, number 49.  Thus we can eliminate the
                  ;; number of empty cells in that region simply by
                  ;; removing the first element.
                  empty-sizes-except-bottom (rest empty-region-sizes)
                  j (int (bit-shift-right board 5))
                  i (int (bit-and board (int 0x1F)))]
              (when-not (every? #(zero? (rem % 5)) empty-sizes-except-bottom)
                ;; then it is possible for pieces to fill the empty
                ;; regions
                (aset bad-even-triples j
                      (bit-or (aget bad-even-triples j)
                              (bit-shift-left (int 1) i)))))))))
    ;; Fill rows 0..4 completely.
    (dotimes [i 25]
      (aset temp-arr i (int 1)))
    (dotimes [row5 32]
      (dotimes [row6 32]
        (dotimes [row7 32]
          (let [board-rows-1-3 (int (bit-or (bit-or row5 (bit-shift-left row6 5))
                                            (bit-shift-left row7 10)))]
            (dotimes [i 15]
              (aset temp-arr (+ (int 25) i)
                    (int (bit-and (int 1) (bit-shift-right board-rows-1-3 i)))))
            (dotimes [i 10]  ;; Rows 8 and 9 are completely empty to start with
              (aset temp-arr (+ (int 40) i) (int 0)))
            (let [empty-region-sizes (board-empty-region-sizes!
                                      temp-arr shift-table oob-table)
                  empty-sizes-except-bottom (rest empty-region-sizes)
                  j (int (bit-shift-right board-rows-1-3 5))
                  i (int (bit-and board-rows-1-3 (int 0x1F)))]
              (when-not (every? #(zero? (rem % 5)) empty-sizes-except-bottom)
                (aset bad-odd-triples j
                      (bit-or (aget bad-odd-triples j)
                              (bit-shift-left 1 i)))
                ))))))
    [bad-even-triples bad-odd-triples]))


(def num-solutions (int-array 1))
(def all-solutions (object-array 2200))

;; See comments above *piece-num-to-do-only-3-rotations*.  Each
;; solution is thus recorded twice.  Reversing the solution has the
;; effect of rotating it 180 degrees.

(defn record-solution! [^ints soln]
  (let [^ints num-solutions num-solutions
        ^objects all-solutions all-solutions
        n (int (aget num-solutions (int 0)))
        ^ints rotated-soln (aclone soln)
        len (int (alength soln))
        len-1 (int (dec len))]
    (aset all-solutions n soln)
    (dotimes [i (unchecked-divide len (int 2))]
      (let [tmp (int (aget rotated-soln i))
            other-idx (int (unchecked-subtract len-1 i))]
        (aset rotated-soln i (aget rotated-soln other-idx))
        (aset rotated-soln other-idx tmp)))
    (aset all-solutions (inc n) rotated-soln)
    (aset num-solutions (int 0) (+ n (int 2)))))


;; Assume all args have been type-hinted to int in the environment
;; where the macro board-has-no-islands is called.

(defmacro board-has-no-islands [board0 board1 index
                                ^ints bad-even-triples
                                ^ints bad-odd-triples]
  `(if (>= ~index (int 40))
     true
     (let [row-num# (int (unchecked-divide ~index (int 5)))
           current-3-rows#
           (int (case row-num#
                      (int 0) (bit-and (int 0x7FFF) ~board0)
                      (int 1) (bit-and (int 0x7FFF)
                                       (bit-shift-right ~board0 (int  5)))
                      (int 2) (bit-and (int 0x7FFF)
                                       (bit-shift-right ~board0 (int 10)))
                      (int 3) (bit-or (bit-shift-right ~board0 (int 15))
                                      (bit-shift-left (bit-and (int 0x1F)
                                                               ~board1)
                                                      (int 10)))
                      (int 4) (bit-or (bit-shift-right ~board0 (int 20))
                                      (bit-shift-left (bit-and (int 0x3FF)
                                                               ~board1)
                                                      (int 5)))
                      (int 5) (bit-and (int 0x7FFF) ~board1)
                      (int 6) (bit-and (int 0x7FFF)
                                       (bit-shift-right ~board1 (int  5)))
                      (int 7) (bit-and (int 0x7FFF)
                                       (bit-shift-right ~board1 (int 10)))))
           int-num# (int (bit-shift-right current-3-rows# (int 5)))
           bit-num# (int (bit-and current-3-rows# (int 0x1F)))
           even-row# (zero? (bit-and row-num# (int 1)))]
       (if even-row#
         (zero? (bit-and (int 1)
                         (bit-shift-right (aget ~bad-even-triples int-num#)
                                          bit-num#)))
         (zero? (bit-and (int 1)
                         (bit-shift-right (aget ~bad-odd-triples int-num#)
                                          bit-num#)))))))


;; Arguments to solve-helper:

;; depth is 0 on the first call, and is 1 more for each level of
;; nested recursive call.  It is equal to the number of pieces placed
;; on the board in the partial solution so far.

;; board is a pair of 32-bit ints representing which board cells are
;; occupied (bit value 1) or empty (bit value 0), based upon the
;; pieces placed so far.  Bits positions 0..24 of board0 represent
;; board indices 0..24, and bit positions 0..24 of board1 represent
;; board indices 25..49.

;; cell is the board index in [0,49] that should be checked first to
;; see if it is empty.

;; placed-piece-bit-vec is an int where its 10 least significant bits
;; represent the set of the piece numbers, each in the range [0,9],
;; that have been placed so far in the current configuration.  If bit
;; i is 1, i in [0,9], then piece i has already been placed.

;; piece-num-arr is an array of the piece-nums placed so far, in the
;; order they were placed, i.e. depth order.  (aget piece-num-arr 0)
;; was placed at depth 0, etc.  (named sol_nums in GCC program)

;; mask-arr is an array of the bitmasks of the pieces placed so far,
;; in the order they were placed.  (named sol_masks in GCC program)

(defn solve! [^objects tbl ^ints bad-even-triples ^ints bad-odd-triples]
  (letfn
      [(solve-helper [depth board0 board1 orig-cell placed-piece-bit-vec
                      ^ints piece-num-arr ^ints mask-arr0 ^ints mask-arr1]
         (let [depth (int depth)
               board0 (int board0)
               board1 (int board1)
               orig-cell (int orig-cell)
               cell (int (first-empty-index orig-cell board0 board1))
               placed-piece-bit-vec-int (int placed-piece-bit-vec)]
           (loop [piece-num (int 0)
                  piece-num-mask (int 1)]
             (when (< piece-num (int 10))
               (when (zero? (bit-and placed-piece-bit-vec-int piece-num-mask))
                 (let [^objects piece-arr (aget tbl piece-num)
                       ^objects placements (aget piece-arr cell)]
                   (dotimes [i (alength placements)]
                     (let [^ints placement (aget placements i)
                           piece-mask0 (int (aget placement (int 0)))
                           piece-mask1 (int (aget placement (int 1)))
                           next-index (int (aget placement (int 2)))
                           piece-num-int (int piece-num)]
                       (when (and (zero? (bit-and board0 piece-mask0))
                                  (zero? (bit-and board1 piece-mask1)))
                         (if (== depth 9)
                           ;; Solution found!
                           (do
                             (aset piece-num-arr depth piece-num-int)
                             (aset mask-arr0 depth piece-mask0)
                             (aset mask-arr1 depth piece-mask1)
                             (let [sol1 (encode-solution piece-num-arr
                                                         mask-arr0 mask-arr1)]
                               (record-solution! sol1)))
                           ;; else
                           (let [next-board0 (int (bit-or board0 piece-mask0))
                                 next-board1 (int (bit-or board1 piece-mask1))]
                             (when (board-has-no-islands next-board0 next-board1
                                                         next-index
                                                         bad-even-triples
                                                         bad-odd-triples)
                               (aset piece-num-arr depth piece-num-int)
                               (aset mask-arr0 depth piece-mask0)
                               (aset mask-arr1 depth piece-mask1)
                               (solve-helper
                                (inc depth)
                                next-board0 next-board1
                                next-index
                                (bit-or placed-piece-bit-vec-int
                                        (bit-shift-left (int 1) piece-num-int))
                                piece-num-arr
                                mask-arr0 mask-arr1)))))))))
               (recur (inc piece-num) (bit-shift-left piece-num-mask (int 1)))
               ))))]
    (solve-helper 0 0 0 0 0 (int-array 10) (int-array 10) (int-array 10))))


(defn compare-int-arrays [^ints a ^ints b]
  (let [len (int (min (alength a) (alength b)))]
    (loop [i (int 0)]
      (if (< i len)
        (let [elem-a (int (aget a i))
              elem-b (int (aget b i))]
          (if (== elem-a elem-b)
            (recur (inc i))
            (unchecked-subtract elem-a elem-b)))
        0))))


(defn -main [& args]
  (let [shift-table (make-shift-table)
        oob-table (make-oob-table)
        tbl (calc-pieces piece-defs shift-table oob-table)
        [bad-even-triples bad-odd-triples] (create-triples shift-table
                                                           oob-table)]
    (solve! tbl bad-even-triples bad-odd-triples)
    (let [^ints num-solutions num-solutions
          n (int (aget num-solutions (int 0)))
          sorted-solns (sort compare-int-arrays (take n (seq all-solutions)))]
      (println (format "%d solutions found" n))
      (print-board (first sorted-solns))
      (println)
      (print-board (nth sorted-solns (dec n)))
      (println)
      (println))))  ; Just to match the output of the other programs exactly
