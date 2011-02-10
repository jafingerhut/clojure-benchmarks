;; The Computer Language Benchmarks Game
;; http://shootout.alioth.debian.org/
;
;; Adapted from the Java -server version
;
;; contributed by Marko Kocic
;; modified by Kenneth Jonsson, restructured to allow usage of 'pmap'
;; modified by Andy Fingerhut to use faster primitive math ops,
;; to use deftype instead of defrecord, and speed up bottom-up-tree.

;; In my measurements on a 4-core 64-bit Ubuntu Linux VM, this version
;; was actually slightly slower (measured by user+system CPU time)
;; than binarytrees.clj-4.clj when the pmap is replaced with map, and
;; about the same speed (measured by elapsed time) when pmap is used.
;; pmap causes the user+CPU time to go up significantly vs. using map.
;; Elapsed time only reduce modestly when pmap is used in place of
;; map.

(ns binarytrees
  (:gen-class))

(set! *warn-on-reflection* true)

(definterface ITreeNode
  (^int item [])
  (left [])
  (right []))

;; These TreeNode's take up noticeably less memory than a similar one
;; implemented using defrecord.

(deftype TreeNode [left right ^int item]
  ITreeNode
  (^int item [this] item)
  (left [this] left)
  (right [this] right))

;; OK, bottom-up-tree is not pretty at all.  It is designed to return
;; the same value that this easier-to-understand function would:

;;(defn bottom-up-tree [item depth]
;;  (let [int-item (int item)
;;        int-depth (int depth)]
;;    (if (zero? int-depth)
;;      (TreeNode. nil nil int-item)
;;      (TreeNode.
;;       (bottom-up-tree (unchecked-dec (unchecked-multiply 2 int-item))
;;                       (unchecked-dec int-depth))
;;       (bottom-up-tree (unchecked-multiply 2 int-item)
;;                       (unchecked-dec int-depth))
;;       int-item))))

;; The main difference is that to avoid the boxing/unboxing of the
;; integer arguments on each call, we use iteration with our own
;; stack, implemented with the stack pointer 'depth' and the variables
;; with names ending in 'stack' holding the data pushed onto the
;; stack.

(defn bottom-up-tree [item depth]
  (let [starting-depth (int depth)
        item (int item)
        itemstack (int-array (inc starting-depth))
        stepstack (int-array (inc starting-depth))
        leftchildstack (object-array (inc starting-depth))]
    (aset itemstack starting-depth item)
    (loop [depth (int depth)
           tree-returned nil]
      (let [item (int (aget itemstack depth))]
        (if (zero? depth)
          ;; build leaf node and 'return' by popping
          (recur (inc depth) (TreeNode. nil nil item))
          ;; else what to do depends on which step we are at
          (let [step (aget stepstack depth)
                double-item (int (* (int 2) item))
                depth-1 (int (dec depth))]
            (case step
                  0 ;; push and recursively build left subtree
                  (do
                    ;; first increment our step so we do the next one
                    ;; after popping back to this level.
                    (aset stepstack depth (int 1))
                    ;; store left item value in itemstack, and step=0
                    ;; in stepstack, before simulated recursive call
                    (aset itemstack depth-1 (dec double-item))
                    (aset stepstack depth-1 (int 0))
                    ;; do the call
                    (recur (dec depth) nil))
                  1 ;; store the left child returned, and then push
                    ;; and recursively build right subtree
                  (do
                    ;; first increment our step so we do the next one
                    ;; after popping back to this level.
                    (aset stepstack depth (int 2))
                    ;; store the 'return value' in our leftchildstack
                    (aset leftchildstack depth tree-returned)
                    ;; store right item value in itemstack, and step=0
                    ;; in stepstack, before simulated recursive call
                    (aset itemstack depth-1 double-item)
                    (aset stepstack depth-1 (int 0))
                    ;; do the call
                    (recur (dec depth) nil))
                  2 ;; combine the left child stored on the stack with
                    ;; the right child just returned in order to
                    ;; create the node we will now return.
                  (do
                    ;; no need to increment our step.  We are done,
                    ;; and popping up to the next stack frame, which
                    ;; has its own step value.  Of course, we may be
                    ;; the bottom stack frame, and we need to check
                    ;; for that case.
                    (let [t (TreeNode. (aget leftchildstack depth)
                                       tree-returned item)]
                      (if (== depth starting-depth)
                        t
                        (recur (inc depth) t)))))))))))


(defn item-check [^TreeNode node]
  (if (nil? (.left node))
    (int (.item node))
    (unchecked-add (unchecked-add (int (.item node))
                                  (int (item-check (.left node))))
                   (int (- (item-check (.right node)))))))

(defn iterate-trees [mx mn d]
  (let [iterations (bit-shift-left 1 (int (+ mx mn (- d))))]
    (format "%d\t trees of depth %d\t check: %d" (* 2 iterations) d
            (reduce + (map (fn [i]
                             (unchecked-add (int (item-check (bottom-up-tree i d)))
                                            (int (item-check (bottom-up-tree (- i) d)))))
                           (range 1 (inc iterations)))))))

(def min-depth 4)

(defn main [max-depth]
  (let [stretch-depth (inc max-depth)]
    (let [tree (bottom-up-tree 0 stretch-depth)
          check (item-check tree)]
      (println (format "stretch tree of depth %d\t check: %d" stretch-depth check)))
    (let [long-lived-tree (bottom-up-tree 0 max-depth) ]
      ;; The following line is where Kenneth Jonsson used pmap.  On a
      ;; 1-core machine, I have found significantly less user+system
      ;; CPU time used when it is map, and slightly less elapsed time
      ;; (at the cost of more user+system CPU time) when it is pmap.
      (doseq [trees-nfo (map (fn [d]
                                (iterate-trees max-depth min-depth d))
			      (range min-depth stretch-depth 2)) ]
        (println trees-nfo))
      (println (format "long lived tree of depth %d\t check: %d" max-depth (item-check long-lived-tree)))
      (shutdown-agents))))

(defn -main [& args]
  (let [n (if (first args) (Integer/parseInt (first args)) 0)
        max-depth (if (> (+ min-depth 2) n) (+ min-depth 2) n)]
    (main max-depth)))
