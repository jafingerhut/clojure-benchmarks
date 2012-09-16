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

(defrecord TreeNode [left right ^int item])

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

(defn item-check [node]
  (if (nil? (:left node))
    (int (:item node))
    (unchecked-add (unchecked-add (int (:item node))
                                  (int (item-check (:left node))))
                   (int (- (item-check (:right node)))))))

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
      (doseq [trees-nfo (pmap (fn [d]
                                (iterate-trees max-depth min-depth d))
			      (range min-depth stretch-depth 2)) ]
        (println trees-nfo))
      (println (format "long lived tree of depth %d\t check: %d" max-depth (item-check long-lived-tree)))
      (shutdown-agents))))

(defn -main [& args]
  (let [n (if (first args) (Integer/parseInt (first args)) 0)
        max-depth (if (> (+ min-depth 2) n) (+ min-depth 2) n)]
    (main max-depth)))
