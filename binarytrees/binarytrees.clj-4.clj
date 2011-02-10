;; The Computer Language Benchmarks Game
;; http://shootout.alioth.debian.org/
;
;; Adapted from the Java -server version
;
;; contributed by Marko Kocic
;; modified by Kenneth Jonsson, restructured to allow usage of 'pmap'
;; modified by Andy Fingerhut to use faster primitive math ops, and
;; deftype instead of defrecord for smaller tree nodes.

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

(defn bottom-up-tree [item depth]
  (let [int-item (int item)
        int-depth (int depth)]
    (if (zero? int-depth)
      (TreeNode. nil nil int-item)
      (TreeNode.
       (bottom-up-tree (unchecked-dec (unchecked-multiply 2 int-item))
                       (unchecked-dec int-depth))
       (bottom-up-tree (unchecked-multiply 2 int-item)
                       (unchecked-dec int-depth))
       int-item))))

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
