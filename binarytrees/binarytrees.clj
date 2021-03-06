;; The Computer Language Benchmarks Game

;; http://shootout.alioth.debian.org/


;; contributed by Marko Kocic 

;; Adapted from the Java -server version


(ns binarytrees
  (:gen-class))

(set! *warn-on-reflection* true)

(defrecord TreeNode [left right ^int item])

(defn bottom-up-tree [item depth]
  (if (> depth 0)
    (TreeNode.
     (bottom-up-tree (dec (* 2 item)) (dec depth))
     (bottom-up-tree (* 2 item) (dec depth))
     item)
    (TreeNode. nil nil item)))

(defn item-check [node]
  (if (nil? (:left node))
    (:item node)
    (+ (:item node) (item-check (:left node)) (- (item-check (:right node))))))


(defn iterate-trees [mx mn d] 
  (let [iterations (bit-shift-left 1 (int (+ mx mn (- d))))] 
    (println (format "%d\t trees of depth %d\t check: %d" (* 2 iterations) d 
             (reduce + (map (fn [i] 
                              (+ (item-check (bottom-up-tree i d)) 
                                 (item-check (bottom-up-tree (- i) d)))) 
                            (range 1 (inc iterations))))))))

(def min-depth 4)

(defn main [max-depth]
  (let [stretch-depth (inc max-depth)]
    (let [tree (bottom-up-tree 0 stretch-depth)
          check (item-check tree)]
      (println (format "stretch tree of depth %d\t check: %d" stretch-depth check)))
    (let [long-lived-tree (bottom-up-tree 0 max-depth)]
      (doseq [d (range min-depth stretch-depth 2)]
        (iterate-trees max-depth min-depth d))
      (println (format "long lived tree of depth %d\t check: %d" max-depth (item-check long-lived-tree))))))

(defn -main [& args]
  (let [n (if (first args) (Integer/parseInt (first args)) 0)
        max-depth (if (> (+ min-depth 2) n) (+ min-depth 2) n)]
    (main max-depth)))
