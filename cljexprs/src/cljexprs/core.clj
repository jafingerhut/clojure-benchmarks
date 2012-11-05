(ns cljexprs.core
  (:require [criterium.core :as criterium]
            [clojure.string :as str]
            [clojure.pprint :as pp]))

(set! *warn-on-reflection* true)


(def ^:dynamic *auto-flush* true)

(defn printf-to-writer [w fmt-str & args]
  (binding [*out* w]
    (apply clojure.core/printf fmt-str args)
    (when *auto-flush* (flush))))

(defn iprintf [fmt-str-or-writer & args]
  (if (instance? CharSequence fmt-str-or-writer)
    (apply printf-to-writer *out* fmt-str-or-writer args)
    (apply printf-to-writer fmt-str-or-writer args)))

(defn die [fmt-str & args]
  (apply iprintf *err* fmt-str args)
  (System/exit 1))

(defn basename
  "If the string contains one or more / characters, return the part of
  the string after the last /.  If it contains no / characters, return
  the entire string."
  [s]
  (if-let [[_ base] (re-matches #".*/([^/]+)" s)]
    base
    s))


(defn time-with-scale [time-sec]
  (cond (< time-sec 1.0e-12) (format "%e sec" time-sec)
        (< time-sec 1.0e-9) (format "%.1f psec" (* 1e12 time-sec))
        (< time-sec 1.0e-6) (format "%.1f nsec" (* 1e9 time-sec))
        (< time-sec 1.0e-3) (format "%.1f usec" (* 1e6 time-sec))
        (< time-sec 1.0) (format "%.1f msec" (* 1e3 time-sec))
        :else (format "%.1f sec" time-sec)))


(defn first-word [s]
  (first (str/split s #"\s+")))


(defmacro benchmark
  [bindings expr & opts]
  `(do
;;     (iprintf *err* "\n")
;;     (iprintf *err* "----------------------------------------\n")
     (iprintf *err* "Benchmarking %s %s ..." '~bindings '~expr)
;;     (iprintf *err* "\n")

     ;(criterium/quick-bench ~expr ~@opts)
     ;(criterium/bench ~expr ~@opts)
     (let ~bindings
       (let [results#
             (criterium/with-progress-reporting
               (criterium/benchmark ~expr ~@opts))
             os# (:os-details results#)
             runtime# (:runtime-details results#)]
         (pp/pprint {:bindings '~bindings
                     :expr '~expr
                     :opts '~opts
                     :results results#})
         (iprintf *err* " %s\n" (time-with-scale (first (:mean results#))))
         (iprintf *err* "    %s\n"
                  (format "Clojure %s on %s-bit %s JDK %s on %s %s"
                          (:clojure-version-string runtime#)
                          (:sun-arch-data-model runtime#)
                          (first-word (:vm-vendor runtime#)) (:java-version runtime#)
                          (:name os#) (:version os#)))))
     (flush)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Begin definitions used below for cljs-bench benchmarks
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord Foo [bar baz])

(def map1 (into {}
                [[:a 0] [:b 1] [:c 2] [:d 3] [:e 4] [:f 5] [:g 6] [:h 7]
                 [:i 8] [:j 9] [:k 10] [:l 11] [:m 12] [:n 13] [:o 14] [:p 15]
                 [:q 16] [:r 17] [:s 18] [:t 19] [:u 20] [:v 21] [:w 22] [:x 23]
                 [:y 24] [:z 25] [:a0 26] [:b0 27] [:c0 28] [:d0 29] [:e0 30] [:f0 31]]))

(defn ints-seq
  ([n] (ints-seq 0 n))
  ([i n]
     (when (< i n)
       (lazy-seq
        (cons i (ints-seq (inc i) n))))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; End definitions used below for cljs-bench benchmarks
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main [& args]
  ;; Charts to add:

  ;; + size in Mbytes of clojure.jar
  ;; + time to build clojure.jar - "ant jar" elapsed time
  ;; + time to run tests on clojure.jar - "ant" elapsed time minus
  ;;   "ant jar" elapsed time (?)
  
  ;; Note: It seems that (set v) is significantly slower than (into
  ;; #{} v) for the vec13 and vec10k cases.  This is reasonable given
  ;; that (into #{} v) uses transients, whereas (set v) is not.

  ;; (set v) is faster for the vec1 and vec3 cases, probably due to
  ;; the constant time overheads the transient operations of (into #{}
  ;; v).

  ;; Is there a relatively simple way to rewrite (set v) so that for
  ;; very short input sequences, say 5 or less, it works as it does
  ;; today, but for longer ones it works the same as (into #{} v)?  If
  ;; v is a collection from which we can get its count in O(1) time,
  ;; that sounds reasonable, but what if it is a lazy sequence or
  ;; other collection that count requires a linear scan on?  Probably
  ;; need some more experimentation there, so that the tests to
  ;; determine which method to use don't add much run time themselves.

  ;; TBD: Do similar tests for other collection constructors, e.g. for
  ;; vectors, maps, etc.
  (benchmark [coll (list 1 2 3)] (first coll))
  (benchmark [coll (list 1 2 3)] (dotimes [i 1000000] (first coll)))
  (benchmark [coll (list 1 2 3)] (rest coll))
  (benchmark [coll (list 1 2 3)] (dotimes [i 1000000] (rest coll)))
  (benchmark [coll [1 2 3]] (conj coll 4))
  (benchmark [coll [1 2 3]] (dotimes [i 1000000] (conj coll 4)))
  (benchmark [v13 ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m"]]
             (count (into #{} v13)))
  (benchmark [v13 ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m"]]
             (dotimes [i 1000000] (count (into #{} v13))))
  (benchmark [v13 ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m"]]
             (into #{} v13))
  (benchmark [v13 ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m"]]
             (dotimes [i 1000000] (into #{} v13)))
  (System/exit 0)

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Begin section that is a translation of cljs-bench benchmarks
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  ;;(simple-benchmark-data [x 1] (identity x) 1000000)
  (benchmark [x 1] (identity x))

  ;;(simple-benchmark-data [coll (seq arr)] (ci-reduce coll + 0) 1)
  (benchmark [arr (object-array (range 1000000))] (reduce arr + 0))
  ;;(simple-benchmark-data [coll (seq arr)] (ci-reduce coll sum 0) 1)
  (benchmark [arr (object-array (range 1000000))] (reduce arr + 0N))
  ;;(simple-benchmark-data [coll arr] (array-reduce coll + 0) 1)
  (benchmark [arr (long-array (range 1000000))] (areduce arr i sum 0 (+ sum i)))
  ;;(simple-benchmark-data [coll arr] (array-reduce coll sum 0) 1)
  (benchmark [arr (long-array (range 1000000))] (areduce arr i sum 0N (+ sum i)))
  ;;(simple-benchmark-data [coll []] (instance? PersistentVector coll) 1000000)
  (benchmark [coll []] (instance? clojure.lang.IPersistentVector coll))

  ;;(simple-benchmark-data [coll (list 1 2 3)] (satisfies? ISeq coll) 1000000)
  (benchmark [coll (list 1 2 3)] (instance? clojure.lang.ISeq coll))
  ;;(simple-benchmark-data [coll [1 2 3]] (satisfies? ISeq coll) 1000000)
  (benchmark [coll [1 2 3]] (instance? clojure.lang.ISeq coll))

  ;;(simple-benchmark-data [coll (list 1 2 3)] (first coll) 1000000)
  (benchmark [coll (list 1 2 3)] (first coll))
  ;;(simple-benchmark-data [coll (list 1 2 3)] (-first coll) 1000000)
  ;; TBD: Any Clojure/JVM equivalent?
  ;;(simple-benchmark-data [coll (list 1 2 3)] (rest coll) 1000000)
  (benchmark [coll (list 1 2 3)] (rest coll))
  ;;(simple-benchmark-data [coll (list 1 2 3)] (-rest coll) 1000000)
  ;; TBD: Any Clojure/JVM equivalent?
  ;;(simple-benchmark-data [] (list) 1000000)
  (benchmark [] (list))
  ;;(simple-benchmark-data [] (list 1 2 3) 1000000)
  (benchmark [] (list 1 2 3))


  ;; vector ops
  ;;(simple-benchmark-data [] [] 1000000)
  (benchmark [] [])
  ;;(simple-benchmark-data [] [1 2 3] 1000000)
  (benchmark [] [1 2 3])
  ;;(simple-benchmark-data [coll [1 2 3]] (transient coll) 100000)
  (benchmark [coll [1 2 3]] (transient coll))
  ;;(simple-benchmark-data [coll [1 2 3]] (nth coll 0) 1000000)
  (benchmark [coll [1 2 3]] (nth coll 0))
  ;;(simple-benchmark-data [coll [1 2 3]] (-nth coll 0) 1000000)
  ;; TBD: Any Clojure/JVM equivalent?
  ;;(simple-benchmark-data [coll [1 2 3]] (conj coll 4) 1000000)
  (benchmark [coll [1 2 3]] (conj coll 4))
  ;;(simple-benchmark-data [coll [1 2 3]] (-conj coll 4) 1000000)
  ;; TBD: Any Clojure/JVM equivalent?
  ;;(simple-benchmark-data [coll [1 2 3]] (seq coll) 1000000)
  (benchmark [coll [1 2 3]] (seq coll))
  ;;(simple-benchmark-data [coll (seq [1 2 3])] (first coll) 1000000)
  (benchmark [coll (seq [1 2 3])] (first coll))
  ;;(simple-benchmark-data [coll (seq [1 2 3])] (-first coll) 1000000)
  ;; TBD: Any Clojure/JVM equivalent?
  ;;(simple-benchmark-data [coll (seq [1 2 3])] (rest coll) 1000000)
  (benchmark [coll (seq [1 2 3])] (rest coll))
  ;;(simple-benchmark-data [coll (seq [1 2 3])] (-rest coll) 1000000)
  ;; TBD: Any Clojure/JVM equivalent?
  ;;(simple-benchmark-data [coll (seq [1 2 3])] (next coll) 1000000)
  (benchmark [coll (seq [1 2 3])] (next coll))

  ;; large vector ops
  ;;(simple-benchmark-data [] (reduce conj [] (range 40000)) 10)
  (benchmark [] (reduce conj [] (range 40000)))
  (benchmark [] (persistent! (reduce conj! (transient []) (range 40000))))
  ;;(simple-benchmark-data [coll (reduce conj [] (range (+ 32768 32)))] (conj coll :foo) 100000)
  (benchmark [coll (into [] (range (+ 32768 32)))] (conj coll :foo))
  ;;(simple-benchmark-data [coll (reduce conj [] (range 40000))] (assoc coll 123 :foo) 100000)
  (benchmark [coll (into [] (range 40000))] (assoc coll 123 :foo))
  ;;(simple-benchmark-data [coll (reduce conj [] (range (+ 32768 33)))] (pop coll) 100000)
  (benchmark [coll (into [] (range (+ 32768 33)))] (pop coll))

  ;; lazy seq reduce
  ;;(simple-benchmark-data [coll (take 100000 (iterate inc 0))] (reduce + 0 coll) 1)
  (benchmark [coll (take 100000 (iterate inc 0))] (reduce + 0 coll))
  ;;(simple-benchmark-data [coll (range 1000000)] (reduce + 0 coll) 1)
  (benchmark [coll (range 1000000)] (reduce + 0 coll))
  ;;(simple-benchmark-data [coll (into [] (range 1000000))] (reduce + 0 coll) 1)
  (benchmark [coll (into [] (range 1000000))] (reduce + 0 coll))

  ;; apply
  ;;(simple-benchmark-data [coll (into [] (range 1000000))] (apply + coll) 1)
  (benchmark [coll (into [] (range 1000000))] (apply + coll))

  ;;(simple-benchmark-data [coll {:foo 1 :bar 2}] (get coll :foo) 1000000)
  (benchmark [coll {:foo 1 :bar 2}] (get coll :foo))
  ;;(simple-benchmark-data [coll {:foo 1 :bar 2}] (-lookup coll :foo nil) 1000000)
  ;; TBD: Any Clojure/JVM equivalent?
  ;;(simple-benchmark-data [coll {:foo 1 :bar 2}] (:foo coll) 1000000)
  (benchmark [coll {:foo 1 :bar 2}] (:foo coll))
  ;;(defrecord Foo [bar baz])
  ;; Copied above defn -main
  ;;(simple-benchmark-data [coll (Foo. 1 2)] (:bar coll) 1000000)
  (benchmark [coll (Foo. 1 2)] (:bar coll))
  ;;(simple-benchmark-data [coll {:foo 1 :bar 2}] (assoc coll :baz 3) 100000)
  (benchmark [coll {:foo 1 :bar 2}] (assoc coll :baz 3))
  ;;(simple-benchmark-data [coll {:foo 1 :bar 2}] (assoc coll :foo 2) 100000)
  (benchmark [coll {:foo 1 :bar 2}] (assoc coll :foo 2))

  ;;(simple-benchmark-data [key :f0] (hash key) 100000)
  (benchmark [key :f0] (hash key))
  ;;(simple-benchmark-data [coll {:foo 1 :bar 2}]
  ;;  (loop [i 0 m coll]
  ;;    (if (< i 100000)
  ;;      (recur (inc i) (assoc m :foo 2))
  ;;      m))
  ;;  1)
  (benchmark [coll {:foo 1 :bar 2}]
             (loop [i 0 m coll]
               (if (< i 100000)
                 (recur (inc i) (assoc m :foo 2))
                 m)))

  ;;(def pmap (into cljs.core.PersistentHashMap/EMPTY
  ;;                [[:a 0] [:b 1] [:c 2] [:d 3] [:e 4] [:f 5] [:g 6] [:h 7]
  ;;                 [:i 8] [:j 9] [:k 10] [:l 11] [:m 12] [:n 13] [:o 14] [:p 15]
  ;;                 [:q 16] [:r 17] [:s 18] [:t 19] [:u 20] [:v 21] [:w 22] [:x 23]
  ;;                 [:y 24] [:z 25] [:a0 26] [:b0 27] [:c0 28] [:d0 29] [:e0 30] [:f0 31]]))
  ;; Copied above defn -main, except renamed map1 to avoid conflict with clojure.core/pmap
  
  ;;(simple-benchmark-data [coll pmap] (:f0 coll) 1000000)
  (benchmark [coll map1] (:f0 coll))
  ;;(simple-benchmark-data [coll pmap] (get coll :f0) 1000000)
  (benchmark [coll map1] (get coll :f0))
  ;;(simple-benchmark-data [coll pmap] (-lookup coll :f0 nil) 1000000)
  ;; TBD: Any Clojure/JVM equivalent?
  ;;(simple-benchmark-data [coll pmap] (assoc coll :g0 32) 1000000)
  (benchmark [coll map1] (assoc coll :g0 32))
  ;;(simple-benchmark-data [coll cljs.core.PersistentHashMap/EMPTY] (assoc coll :f0 1) 1000000)
  (benchmark [coll {}] (assoc coll :f0 1))

  ;;(simple-benchmark-data [] #{} 100000)
  (benchmark [] #{})
  ;;(simple-benchmark-data [] #{1 2 3} 100000)
  (benchmark [] #{1 2 3})
  ;;(simple-benchmark-data [coll #{1 2 3}] (conj coll 4) 100000)
  (benchmark [coll #{1 2 3}] (conj coll 4))

  ;;(simple-benchmark-data [coll (range 500000)] (reduce + coll) 1)
  (benchmark [coll (range 500000)] (reduce + coll))

  ;;(simple-benchmark-data [s "{:foo [1 2 3]}"] (reader/read-string s) 1000)
  (benchmark [s "{:foo [1 2 3]}"] (read-string s))

  ;;(simple-benchmark-data [m {:foo [1 2 {:bar {3 :a 4 #{:b :c :d :e}}}]}] (pr-str m) 1000)
  (benchmark [m {:foo [1 2 {:bar {3 :a 4 #{:b :c :d :e}}}]}] (pr-str m))

  ;;(simple-benchmark-data [r (range 1000000)] (last r) 1)
  (benchmark [r (range 1000000)] (last r))

  ;;(defn ints-seq
  ;;  ([n] (ints-seq 0 n))
  ;;  ([i n]
  ;;     (when (< i n)
  ;;       (lazy-seq
  ;;        (cons i (ints-seq (inc i) n))))))
  ;;(def r (ints-seq 1000000))
  ;; Copied above defn -main
  ;;(simple-benchmark-data [r r] (last r) 1)
  (benchmark [r (ints-seq 1000000)] (last r) 1)

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; End section that is a translation of cljs-bench benchmarks
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



  
  
  (System/exit 0)
  (benchmark [v1 ["a"]] (count (into #{} v1)))
  (benchmark [v1 ["a"]] (count (set v1)))
  (System/exit 0)
  (benchmark [v3 ["a" "b" "c"]] (count (into #{} v3)))
  (benchmark [v3 ["a" "b" "c"]] (count (set v3)))
  (benchmark [v13 ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m"]]
             (count (into #{} v13)))
  (benchmark [v13 ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m"]]
             (count (set v13)))
  (benchmark [v10k (vec (range 10000))] (count (into #{} v10k)))
  (benchmark [v10k (vec (range 10000))] (count (set v10k)))
  (System/exit 0)

  ;; TBD: Why is this version 6 times slower than the 2 versions
  ;; below, each of which are the same speed?
  (benchmark []
             (let [n 500000]
               (loop [i 0
                      sum 0]
                 (if (< i n)
                   (recur (inc i) (+ sum i))
                   sum))))
  (System/exit 0)

  (benchmark []
             (let [n (long 500000)]
               (loop [i (long 0)
                      sum (long 0)]
                 (if (< i n)
                   (recur (unchecked-inc i) (unchecked-add sum i))
                   sum))))
  (benchmark []
             (let [n 500000]
               (loop [i 0
                      sum 0]
                 (if (< i n)
                   (recur (unchecked-inc i) (unchecked-add sum i))
                   sum))))
  (System/exit 0)
  )
