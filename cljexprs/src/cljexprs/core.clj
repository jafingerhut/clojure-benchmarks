(ns cljexprs.core
  (:require [criterium.core :as criterium]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]))

(set! *warn-on-reflection* true)

(def ^:dynamic *sample-count* 30)

;; time units for values below are nanosec
(def s-to-ns (* 1000 1000 1000))

;; No warmup except the code that estimates the number of executions
;; of the expression needed to take about *target-execution-time*
;;(def ^:dynamic *warmup-jit-period* (long (*  0 s-to-ns)))
;; criterium default = 10 sec
(def ^:dynamic *warmup-jit-period* (long (* 10 s-to-ns)))

;; 100 msec
;;(def ^:dynamic *target-execution-time* (long (* 0.1 s-to-ns)))
;; criterium default = 1 sec
(def ^:dynamic *target-execution-time* (long (* 1.0 s-to-ns)))


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


(defn num-digits-after-dec-point
  [x n]
  (loop [max-val (- 10.0 (/ 5.0 (apply * (repeat n 10.0))))
         digits-after-dec-point (dec n)]
    (if (or (zero? digits-after-dec-point)
            (< x max-val))
      digits-after-dec-point
      (recur (* max-val 10.0) (dec digits-after-dec-point)))))


(defn float-with-n-sig-figs
  "Assumes x >= 1.0 and n >= 1.  Returns a string that represents the
  value of x with at least n significant figures."
  [x n]
  (format (format "%%.%df" (num-digits-after-dec-point x n)) x))


(defn time-with-scale [time-sec]
  (let [[s units scale-fac]
        (cond (< time-sec 999.5e-15) [(format "%.2e sec" time-sec)]
              (< time-sec 999.5e-12) [nil "psec" 1e12]
              (< time-sec 999.5e-9)  [nil "nsec" 1e9]
              (< time-sec 999.5e-6)  [nil "usec" 1e6]
              (< time-sec 999.5e-3)  [nil "msec" 1e3]
              (< time-sec 999.5e+0)  [nil  "sec" 1  ]
              :else [(format "%.0f sec" time-sec)])]
    (if s
      s
      (str (float-with-n-sig-figs (* scale-fac time-sec) 3) " " units))))


(defn first-word [s]
  (first (str/split s #"\s+")))


(defn platform-desc [results]
  (let [os (:os-details results)
        runtime (:runtime-details results)]
    (format "Clojure %s / %s-bit %s JDK %s / %s %s"
            (:clojure-version-string runtime)
            (:sun-arch-data-model runtime)
            (first-word (:vm-vendor runtime))
            (:java-version runtime)
            (:name os) (:version os))))


(defmacro benchmark
  [bindings expr & opts]
  `(do
     (iprintf *err* "Benchmarking %s %s ..." '~bindings '~expr)
     ;(criterium/quick-bench ~expr ~@opts)
     ;(criterium/bench ~expr ~@opts)
     (let ~bindings
       (let [results#
;;             (criterium/with-progress-reporting
               (criterium/benchmark ~expr ~@opts
                                    :samples *sample-count*
                                    :warmup-jit-period *warmup-jit-period*
                                    :target-execution-time *target-execution-time*)
;;               (criterium/quick-benchmark ~expr ~@opts)
;;               )
             ]
         (pp/pprint {:bindings '~bindings
                     :expr '~expr
                     :opts '~opts
                     :results results#})
         (iprintf *err* " %s\n" (time-with-scale (first (:mean results#))))
;;         (iprintf *err* "    %s\n" (platform-desc results#))
         ))
     (flush)))


(defmacro benchmark-round-robin
  [& bindings-expr-pairs]
  (when (odd? (count bindings-expr-pairs))
    (throw (IllegalArgumentException.
            "benchmark-round-robin should have even number of args")))
  `(do
     ;; TBD: Note that right now this simply takes all of the binding
     ;; vectors and concatenates them together into one big vector.
     ;; It would be better to do things like the following, to catch
     ;; mistakes:

     ;; (1) verify each vector has an even number of elements

     ;; (2) verify that for any names being bound (including
     ;; destructuring expression), if the names bound are the same,
     ;; then the expressions bound are the same, too.  That won't
     ;; catch mistakes involving expressions with side effects, but it
     ;; will catch common mistakes of using the same name to mean two
     ;; different things.

     ;; (2b) More difficult would be to try to peek inside
     ;; destructuring expressions and look for duplicate names within
     ;; them.  For now perhaps simply disallow destructuring
     ;; expressions?

     ;; (3) If two sets of names/destructuring expressions and
     ;; expressions to bind to them are the same, only put them in the
     ;; final list of bindings once, not multiple times.

     (let ~(vec (mapcat first (partition 2 bindings-expr-pairs)))
       (let [results#
;;             (criterium/with-progress-reporting
               (criterium/benchmark-round-robin
                ~(map second (partition 2 bindings-expr-pairs))
                {:samples *sample-count*
                 :warmup-jit-period *warmup-jit-period*
                 :target-execution-time *target-execution-time*
                 })
;;               )
             ]
         (doseq [[[bindings# expr#] results#]
                 (map vector '~(partition 2 bindings-expr-pairs) results#)]
           (pp/pprint {:bindings bindings#
                       :expr expr#
                       :opts nil
                       :results results#}))
;;         (iprintf *err* " %s\n" (time-with-scale (first (:mean results#))))
         ))
     (flush)))


(defn report-from-benchmark-results-file [fname]
  (with-open [rdr (java.io.PushbackReader. (io/reader fname))]
    (loop [result (read rdr false :eof)]
      (when-not (= result :eof)
        (iprintf "\n\n")
        (iprintf "Benchmark %s\n" (:bindings result))
        (iprintf "    %s\n" (:expr result))
        (iprintf "    using %s\n" (platform-desc (:results result)))
        (criterium/report-result (:results result))
        (recur (read rdr false :eof))))))


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


(defn run-benchmarks-set1 []
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
             (dotimes [i 1000000] (into #{} v13))))


(defn run-benchmarks-cljs []
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Begin section that is a translation of cljs-bench benchmarks
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  ;;(simple-benchmark-data [x 1] (identity x) 1000000)
  (benchmark [x 1] (identity x))

  ;;(simple-benchmark-data [coll (seq arr)] (ci-reduce coll + 0) 1)
  (benchmark [arr (object-array (range 1000000))] (reduce + 0 arr))
  ;;(simple-benchmark-data [coll (seq arr)] (ci-reduce coll sum 0) 1)
  (benchmark [arr (object-array (range 1000000))] (reduce + 0N arr))
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
  ;; TBD: The test below is actually slower than the previous one with
  ;; Mac OS X 10.6.8 + Apple JDK 1.6.0_37 64-bit + Clojure 1.3.0 if I
  ;; use -Xmx512m on the java command line (7.65 msec for above, 8.01
  ;; msec for below).  With -Xmx1024m, the one below reduces to 5.54
  ;; msec, but the one above stays the same.  Both of them frequently
  ;; use 4 of my 8 cores on the Mac Pro.

  ;; Why does the one below use so much more memory than the one
  ;; above, and is slower unless given significantly more memory to
  ;; work with?

  ;; Why do they both use about 4 cores in parallel so much of the
  ;; time?
  ;;  clj       -Xmx    above      below
  ;; -----     -----  ---------  ---------
  ;; 1.3        512m  7.65 msec  8.01 msec
  ;; 1.3        768m  7.71 msec  6.93 msec
  ;; 1.3       1024m  7.65 msec  5.54 msec
  ;; 1.5-beta1 1024m  7.73 msec  6.86 msec
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
  (benchmark [r (ints-seq 1000000)] (last r))

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; End section that is a translation of cljs-bench benchmarks
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  )


(defn run-benchmarks-cljs-round-robin []
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Begin section that is a translation of cljs-bench benchmarks
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (benchmark-round-robin
   ;;(simple-benchmark-data [x 1] (identity x) 1000000)
   [x 1] (identity x)
  
   ;;(simple-benchmark-data [coll (seq arr)] (ci-reduce coll + 0) 1)
   [obj-arr-1m (object-array (range 1000000))] (reduce + 0 obj-arr-1m)
   ;;(simple-benchmark-data [coll (seq arr)] (ci-reduce coll sum 0) 1)
   [obj-arr-1m (object-array (range 1000000))] (reduce + 0N obj-arr-1m)
   ;;(simple-benchmark-data [coll arr] (array-reduce coll + 0) 1)
   [long-arr-1m (long-array (range 1000000))] (areduce long-arr-1m i sum 0 (+ sum i))
   ;;(simple-benchmark-data [coll arr] (array-reduce coll sum 0) 1)
   [long-arr-1m (long-array (range 1000000))] (areduce long-arr-1m i sum 0N (+ sum i))
   ;;(simple-benchmark-data [coll []] (instance? PersistentVector coll) 1000000)
   [empty-vec []] (instance? clojure.lang.IPersistentVector empty-vec)

   ;;(simple-benchmark-data [coll (list 1 2 3)] (satisfies? ISeq coll) 1000000)
   [list3 (list 1 2 3)] (instance? clojure.lang.ISeq list3)
   ;;(simple-benchmark-data [coll [1 2 3]] (satisfies? ISeq coll) 1000000)
   [vec3 [1 2 3]] (instance? clojure.lang.ISeq vec3)

   ;;(simple-benchmark-data [coll (list 1 2 3)] (first coll) 1000000)
   [list3 (list 1 2 3)] (first list3)
   ;;(simple-benchmark-data [coll (list 1 2 3)] (-first coll) 1000000)
   ;; TBD: Any Clojure/JVM equivalent?
   ;;(simple-benchmark-data [coll (list 1 2 3)] (rest coll) 1000000)
   [list3 (list 1 2 3)] (rest list3)
   ;;(simple-benchmark-data [coll (list 1 2 3)] (-rest coll) 1000000)
   ;; TBD: Any Clojure/JVM equivalent?
   ;;(simple-benchmark-data [] (list) 1000000)
   [] (list)
   ;;(simple-benchmark-data [] (list 1 2 3) 1000000)
   [] (list 1 2 3)


   ;; vector ops
   ;;(simple-benchmark-data [] [] 1000000)
   [] []
   ;;(simple-benchmark-data [] [1 2 3] 1000000)
   [] [1 2 3]
   ;;(simple-benchmark-data [coll [1 2 3]] (transient coll) 100000)
   [vec3 [1 2 3]] (transient vec3)
   ;;(simple-benchmark-data [coll [1 2 3]] (nth coll 0) 1000000)
   [vec3 [1 2 3]] (nth vec3 0)
   ;;(simple-benchmark-data [coll [1 2 3]] (-nth coll 0) 1000000)
   ;; TBD: Any Clojure/JVM equivalent?
   ;;(simple-benchmark-data [coll [1 2 3]] (conj coll 4) 1000000)
   [vec3 [1 2 3]] (conj vec3 4)
   ;;(simple-benchmark-data [coll [1 2 3]] (-conj coll 4) 1000000)
   ;; TBD: Any Clojure/JVM equivalent?
   ;;(simple-benchmark-data [coll [1 2 3]] (seq coll) 1000000)
   [vec3 [1 2 3]] (seq vec3)
   ;;(simple-benchmark-data [coll (seq [1 2 3])] (first coll) 1000000)
   [seq-vec3 (seq [1 2 3])] (first seq-vec3)
   ;;(simple-benchmark-data [coll (seq [1 2 3])] (-first coll) 1000000)
   ;; TBD: Any Clojure/JVM equivalent?
   ;;(simple-benchmark-data [coll (seq [1 2 3])] (rest coll) 1000000)
   [seq-vec3 (seq [1 2 3])] (rest seq-vec3)
   ;;(simple-benchmark-data [coll (seq [1 2 3])] (-rest coll) 1000000)
   ;; TBD: Any Clojure/JVM equivalent?
   ;;(simple-benchmark-data [coll (seq [1 2 3])] (next coll) 1000000)
   [seq-vec3 (seq [1 2 3])] (next seq-vec3)

   ;; large vector ops
   ;;(simple-benchmark-data [] (reduce conj [] (range 40000)) 10)
   [] (reduce conj [] (range 40000))
   ;; TBD: The test below is actually slower than the previous one
   ;; with Mac OS X 10.6.8 + Apple JDK 1.6.0_37 64-bit + Clojure 1.3.0
   ;; if I use -Xmx512m on the java command line (7.65 msec for above,
   ;; 8.01 msec for below).  With -Xmx1024m, the one below reduces to
   ;; 5.54 msec, but the one above stays the same.  Both of them
   ;; frequently use 4 of my 8 cores on the Mac Pro.
   
   ;; Why does the one below use so much more memory than the one
   ;; above, and is slower unless given significantly more memory to
   ;; work with?

   ;; Why do they both use about 4 cores in parallel so much of the
   ;; time?
   ;;  clj       -Xmx    above      below
   ;; -----     -----  ---------  ---------
   ;; 1.3        512m  7.65 msec  8.01 msec
   ;; 1.3        768m  7.71 msec  6.93 msec
   ;; 1.3       1024m  7.65 msec  5.54 msec
   ;; 1.5-beta1 1024m  7.73 msec  6.86 msec
   [] (persistent! (reduce conj! (transient []) (range 40000)))
   ;;(simple-benchmark-data [coll (reduce conj [] (range (+ 32768 32)))] (conj coll :foo) 100000)
   [vec-32k+32 (into [] (range (+ 32768 32)))] (conj vec-32k+32 :foo)
   ;;(simple-benchmark-data [coll (reduce conj [] (range 40000))] (assoc coll 123 :foo) 100000)
   [vec-40k (into [] (range 40000))] (assoc vec-40k 123 :foo)
   ;;(simple-benchmark-data [coll (reduce conj [] (range (+ 32768 33)))] (pop coll) 100000)
   [vec-32k+33 (into [] (range (+ 32768 33)))] (pop vec-32k+33)

   ;; lazy seq reduce
   ;;(simple-benchmark-data [coll (take 100000 (iterate inc 0))] (reduce + 0 coll) 1)
   [lazy-seq-100k (take 100000 (iterate inc 0))] (reduce + 0 lazy-seq-100k)
   ;;(simple-benchmark-data [coll (range 1000000)] (reduce + 0 coll) 1)
   [range-1m (range 1000000)] (reduce + 0 range-1m)
   ;;(simple-benchmark-data [coll (into [] (range 1000000))] (reduce + 0 coll) 1)
   [vec-1m (into [] (range 1000000))] (reduce + 0 vec-1m)

   ;; apply
   ;;(simple-benchmark-data [coll (into [] (range 1000000))] (apply + coll) 1)
   [vec-1m (into [] (range 1000000))] (apply + vec-1m)

   ;;(simple-benchmark-data [coll {:foo 1 :bar 2}] (get coll :foo) 1000000)
   [map-2 {:foo 1 :bar 2}] (get map-2 :foo)
   ;;(simple-benchmark-data [coll {:foo 1 :bar 2}] (-lookup coll :foo nil) 1000000)
   ;; TBD: Any Clojure/JVM equivalent?
   ;;(simple-benchmark-data [coll {:foo 1 :bar 2}] (:foo coll) 1000000)
   [map-2 {:foo 1 :bar 2}] (:foo map-2)
   ;;(defrecord Foo [bar baz])
   ;; Copied above defn -main
   ;;(simple-benchmark-data [coll (Foo. 1 2)] (:bar coll) 1000000)
   [rec-2 (Foo. 1 2)] (:bar rec-2)
   ;;(simple-benchmark-data [coll {:foo 1 :bar 2}] (assoc coll :baz 3) 100000)
   [map-2 {:foo 1 :bar 2}] (assoc map-2 :baz 3)
   ;;(simple-benchmark-data [coll {:foo 1 :bar 2}] (assoc coll :foo 2) 100000)
   [map-2 {:foo 1 :bar 2}] (assoc map-2 :foo 2)

   ;;(simple-benchmark-data [key :f0] (hash key) 100000)
   [key :f0] (hash key)
   ;;(simple-benchmark-data [coll {:foo 1 :bar 2}]
   ;;  (loop [i 0 m coll]
   ;;    (if (< i 100000)
   ;;      (recur (inc i) (assoc m :foo 2))
   ;;      m))
   ;;  1)
   [map-2 {:foo 1 :bar 2}] (loop [i 0 m map-2]
                             (if (< i 100000)
                               (recur (inc i) (assoc m :foo 2))
                               m))
   
   ;;(def pmap (into cljs.core.PersistentHashMap/EMPTY
   ;;                [[:a 0] [:b 1] [:c 2] [:d 3] [:e 4] [:f 5] [:g 6] [:h 7]
   ;;                 [:i 8] [:j 9] [:k 10] [:l 11] [:m 12] [:n 13] [:o 14] [:p 15]
   ;;                 [:q 16] [:r 17] [:s 18] [:t 19] [:u 20] [:v 21] [:w 22] [:x 23]
   ;;                 [:y 24] [:z 25] [:a0 26] [:b0 27] [:c0 28] [:d0 29] [:e0 30] [:f0 31]]))
   ;; Copied above defn -main, except renamed map1 to avoid conflict with clojure.core/pmap
   
   ;;(simple-benchmark-data [coll pmap] (:f0 coll) 1000000)
   [map-32-keyword-keys map1] (:f0 map-32-keyword-keys)
   ;;(simple-benchmark-data [coll pmap] (get coll :f0) 1000000)
   [map-32-keyword-keys map1] (get map-32-keyword-keys :f0)
   ;;(simple-benchmark-data [coll pmap] (-lookup coll :f0 nil) 1000000)
   ;; TBD: Any Clojure/JVM equivalent?
   ;;(simple-benchmark-data [coll pmap] (assoc coll :g0 32) 1000000)
   [map-32-keyword-keys map1] (assoc map-32-keyword-keys :g0 32)
   ;;(simple-benchmark-data [coll cljs.core.PersistentHashMap/EMPTY] (assoc coll :f0 1) 1000000)
   [empty-map {}] (assoc empty-map :f0 1)

   ;;(simple-benchmark-data [] #{} 100000)
   [] #{}
   ;;(simple-benchmark-data [] #{1 2 3} 100000)
   [] #{1 2 3}
   ;;(simple-benchmark-data [coll #{1 2 3}] (conj coll 4) 100000)
   [set3 #{1 2 3}] (conj set3 4)

   ;;(simple-benchmark-data [coll (range 500000)] (reduce + coll) 1)
   [range-500k (range 500000)] (reduce + range-500k)

   ;;(simple-benchmark-data [s "{:foo [1 2 3]}"] (reader/read-string s) 1000)
   [str1 "{:foo [1 2 3]}"] (read-string str1)

   ;;(simple-benchmark-data [m {:foo [1 2 {:bar {3 :a 4 #{:b :c :d :e}}}]}] (pr-str m) 1000)
   [map-nested {:foo [1 2 {:bar {3 :a 4 #{:b :c :d :e}}}]}] (pr-str map-nested)

   ;;(simple-benchmark-data [r (range 1000000)] (last r) 1)
   [range-1m (range 1000000)] (last range-1m)
   
   ;;(defn ints-seq
   ;;  ([n] (ints-seq 0 n))
   ;;  ([i n]
   ;;     (when (< i n)
   ;;       (lazy-seq
   ;;        (cons i (ints-seq (inc i) n))))))
   ;;(def r (ints-seq 1000000))
   ;; Copied above defn -main
   ;;(simple-benchmark-data [r r] (last r) 1)
   [lazy-seq-1m (ints-seq 1000000)] (last lazy-seq-1m)
   
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; End section that is a translation of cljs-bench benchmarks
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ))


;; Note: It seems that (set v) is significantly slower than (into #{}
;; v) for the vec13 and vec10k cases.  This is reasonable given that
;; (into #{} v) uses transients, whereas (set v) is not.

;; (set v) is faster for the vec1 and vec3 cases, probably due to the
;; constant time overheads the transient operations of (into #{} v).

;; Is there a relatively simple way to rewrite (set v) so that for
;; very short input sequences, say 5 or less, it works as it does
;; today, but for longer ones it works the same as (into #{} v)?  If v
;; is a collection from which we can get its count in O(1) time, that
;; sounds reasonable, but what if it is a lazy sequence or other
;; collection that count requires a linear scan on?  Probably need
;; some more experimentation there, so that the tests to determine
;; which method to use don't add much run time themselves.

;; TBD: Be careful of sorted sets, on which the underlying data
;; structure has no corresponding transient version, so transient
;; fails.

;; TBD: Do similar tests for other collection constructors, e.g. for
;; vectors, maps, etc.

(defn run-benchmarks-into-vs-set []
  (benchmark [v1 ["a"]] (count (into #{} v1)))
  (benchmark [v1 ["a"]] (count (set v1)))
  (benchmark [v3 ["a" "b" "c"]] (count (into #{} v3)))
  (benchmark [v3 ["a" "b" "c"]] (count (set v3)))
  (benchmark [v13 ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m"]]
             (count (into #{} v13)))
  (benchmark [v13 ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m"]]
             (count (set v13)))
  (benchmark [v10k (vec (range 10000))] (count (into #{} v10k)))
  (benchmark [v10k (vec (range 10000))] (count (set v10k))))


(defn run-benchmarks-tiny []
  (benchmark [v13 ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m"]]
             (into #{} v13))
  (benchmark [v13 ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m"]]
             (set v13)))


(defn run-benchmarks-tiny-round-robin []
  (benchmark-round-robin
   [v13 ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m"]]
   (into #{} v13)
   [v13 ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m"]]
   (set v13)))


(def benchmark-info
  {"tiny" #(run-benchmarks-tiny)
   "tiny-rr" #(run-benchmarks-tiny-round-robin)
   "set1" #(run-benchmarks-set1)
   "cljs" #(run-benchmarks-cljs)
   "cljs-rr" #(run-benchmarks-cljs-round-robin)
   "into-vs-set" #(run-benchmarks-into-vs-set)})


(defn run-benchmarks [output-file bench-name]
  (if-let [f (get benchmark-info bench-name)]
    (with-open [wrtr (io/writer output-file)]
      (binding [*out* wrtr]
        (f)))
    (do
      (iprintf *err* "Unknown benchmark name '%s'.  Known benchmarks:\n"
               bench-name)
      (iprintf *err* "    %s\n"
               (str/join "\n    " (sort (keys benchmark-info)))))))


(defn replace-clojure-version [project-clj-fname new-clojure-version-str
                               out-fname]
  (let [f (io/file project-clj-fname)]
    (if (.exists f)
      (with-open [wrtr (io/writer out-fname)]
        (let [s (slurp f)
              proj (read-string s)
              [before-deps [_ deps & after-deps]] (split-with
                                                   #(not= % :dependencies)
                                                   proj)
              newdeps (vec (map (fn [[artifact vers]]
                                  [artifact (if (= artifact 'org.clojure/clojure)
                                              new-clojure-version-str
                                              vers)])
                                deps))]
          (binding [*out* wrtr]
            (pp/pprint (concat before-deps [:dependencies newdeps]
                               after-deps)))))
      (iprintf *err* "Could not open file '%s' for reading.\n"
               project-clj-fname))))


;; TBD: Charts to add:

;; + size in Mbytes of clojure.jar
;; + time to build clojure.jar - "ant jar" elapsed time
;; + time to run tests on clojure.jar - "ant" elapsed time minus
;;   "ant jar" elapsed time (?)


(defn show-usage [prog-name]
  (iprintf *err* "usage:
    %s [ help | -h | --help ]
    %s replace-leiningen-project-clojure-version <project.clj> <clj_version_str> <newproject.clj>
    %s benchmark <output_file> <benchmark_set_name>
" prog-name prog-name prog-name))

(def prog-name "lein2 run")


(defn -main [& args]
  (when (or (= 0 (count args))
            (#{"-h" "--help" "-help" "help"} (first args)))
    (show-usage prog-name)
    (System/exit 0))
  (let [[action & args] args]
    (case action

      "replace-leiningen-project-clojure-version"
      (if (= 3 (count args))
        (apply replace-clojure-version args)
        (do (iprintf *err* "Wrong number of args for 'replace-leiningen-project-clojure-version' action\n")
            (show-usage prog-name)
            (System/exit 1)))

      "benchmark"
      (if (= 2 (count args))
        (apply run-benchmarks args)
        (do (iprintf *err* "Wrong number of args for 'benchmark' action\n")
            (show-usage prog-name)
            (System/exit 1)))

      "report"
      (if (= 0 (count args))
        (do (iprintf *err* "Action 'report' needs 1 or more results files.\n")
            (System/exit 1))
        (doseq [fname args]
          (report-from-benchmark-results-file fname)))

      ;; default case
      (do (iprintf *err* "Urecognized first arg '%s'\n" action)
          (show-usage prog-name)
          (System/exit 1)))))
