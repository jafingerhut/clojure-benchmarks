;; The Computer Language Benchmarks Game
;; http://benchmarksgame.alioth.debian.org/

;; Adapted from the Java -server version
;; contributed by Marko Kocic
;; modified by Kenneth Jonsson, restructured to allow usage of 'pmap'
;; modified by Andy Fingerhut to use faster primitive math ops, and
;; deftype instead of defrecord for smaller tree nodes.
;; modified by Rich Hickey for Clojure 1.3
;; modified promise/delivery improvement by Stuart Halloway
;; small hints by David Nolen and Alex Miller
                                        ;
(ns binarytrees
  (:gen-class))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

(def ^:const ^long min-depth 4)

(deftype TreeNode [left right ^long item])

(defn make-tree [^long item ^long depth]
  (if (zero? depth)
    (TreeNode. nil nil item)
    (let [i2 (* 2 item)
          ddec (dec depth)]
      (TreeNode. (make-tree (dec i2) ddec) (make-tree i2 ddec) item))))

(defn item-check ^long [^TreeNode node]
  (if (nil? (.left node))
    (.item node)
    (- (+ (.item node)
          (item-check (.left node)))
       (item-check (.right node)))))

(defn iterate-trees [^long mx ^long mn ^long d]
  (let [iterations (bit-shift-left 1 (long (+ mx mn (- d))))]
    (format "%d\t trees of depth %d\t check: %d"
            (* 2 iterations)
            d
            (loop [result 0
         i 1]
    (if (= i (inc iterations))
      result
      (recur (+ result
                (item-check (make-tree i d))
                (item-check (make-tree (- i) d)))
             (inc i)))))))

(defn main [^long max-depth]
  (let [stretch-depth (inc max-depth)]
    (let [tree (make-tree 0 stretch-depth)
          check (item-check tree)]
      (println (format "stretch tree of depth %d\t check: %d" stretch-depth check)))
    (let [agents (repeatedly (.availableProcessors (Runtime/getRuntime)) #(agent []))
          long-lived-tree (make-tree 0 max-depth)]
      (loop [depth min-depth
             [a & more] (cycle agents)
             results []]
        (if (> depth stretch-depth)
          (doseq [r results] (println @r))
          (let [result (promise)]
            (send a (fn [_]
                      (deliver result (iterate-trees max-depth min-depth depth))))
            (recur (+ 2 depth) more (conj results result)))))
        (println (format "long lived tree of depth %d\t check: %d" max-depth (item-check long-lived-tree))))))

(defn -main [& args]
  (let [n (if (first args) (Long/parseLong (first args)) 0)
        max-depth (if (> (+ min-depth 2) n) (+ min-depth 2) n)]
    (main max-depth)
    (shutdown-agents)))
