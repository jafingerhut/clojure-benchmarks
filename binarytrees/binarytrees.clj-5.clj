;; The Computer Language Benchmarks Game
;; http://shootout.alioth.debian.org/
;
;; Adapted from the Java -server version
;
;; contributed by Marko Kocic
;; modified by Kenneth Jonsson, restructured to allow usage of 'pmap'
;; modified by Andy Fingerhut to use faster primitive math ops

(ns binarytrees
  (:gen-class))

(set! *warn-on-reflection* true)

(definterface ITreeNode
  (^int item [])
  (left [])
  (right []))

(deftype TreeNode [left right ^int item]
  ITreeNode
  (^int item [this] item)
  (left [this] left)
  (right [this] right))

;; Simulate recursion with our own hand-maintained stack.
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
        ;;(println "depth=" depth " item=" item " tree-returned=" tree-returned)
        ;;(println "itemstack=" (seq itemstack))
        ;;(println "stepstack=" (seq stepstack))
        ;;(println "leftchildstack=" (seq leftchildstack))
        ;;(flush)
        (if (zero? depth)
          ;; build leaf node and 'return' by popping
          (do
            ;;(println "building leaf...")
            (recur (inc depth) (TreeNode. nil nil item))
            )
          ;; else what to do depends on which step we are at
          (let [step (aget stepstack depth)
                double-item (int (* (int 2) item))
                depth-1 (int (dec depth))]
            (case step
                  0 ;; push and recursively build left subtree
                  (do
                    ;;(println "doing step 0...")
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
                    ;;(println "doing step 1...")
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
                    ;;(println "doing step 2...")
                    ;; no need to increment our step.  We are done,
                    ;; and popping up to the next stack frame, which
                    ;; has its own step value.  Of course, we may be
                    ;; the root stack frame, and we need to check for
                    ;; that case.
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
