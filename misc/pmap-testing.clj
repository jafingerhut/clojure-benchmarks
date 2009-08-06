;;(set! *warn-on-reflection* true)

(def *default-repetitions* 1000000000)
(def *default-modified-pmap-num-threads*
     (+ 2 (.. Runtime getRuntime availableProcessors)))

(defn usage [exit-code]
  (println (format "usage: %s type num-jobs job-size num-threads" *file*))
  (println (format "    type must be one of int or double"))
  (println (format "    all other arguments must be integers >= 0"))
  (println (format "    num-jobs must be >= 1, and is the number of jobs in the list to perform"))
  (println (format "    job-size is the number of steps in each job"))
  (println (format "        0 means to use the default number of steps: %d"
                   *default-repetitions*))
  (println (format "    num-threads is the number of threads to run in parallel"))
  (println (format "        0 means to use the default number of threads: %d"
                   *default-modified-pmap-num-threads*))
  (println (format "        1 means to use sequential map, guaranteeing no parallelism"))
  (. System (exit exit-code)))

(declare spin-int spin-double)

(when (not= 4 (count *command-line-args*))
  (println (str "Expected 4 args but found " (count *command-line-args*)))
  (usage 1))
(def task-fn-specifier
     (let [arg (nth *command-line-args* 0)]
       (condp = arg
         "int" "int"
         "double" "double"
         :else (do
                 (println "type specified was " arg " but must be one of: int,double")
                 (usage 1)))))
(def num-jobs
     (let [arg (nth *command-line-args* 1)]
       (when (not (re-matches #"^\d+$" arg))
         (println "num-jobs specified was " arg " but must be an integer")
         (usage 1))
       (let [temp (. Integer valueOf arg 10)]
         (when (< temp 1)
           (usage 1))
         temp)))
(def job-size
     (let [arg (nth *command-line-args* 2)]
       (when (not (re-matches #"^\d+$" arg))
         (println "job-size specified was " arg " but must be an integer")
         (usage 1))
       (let [temp (BigInteger. arg)]
         (cond (not= temp (int temp))
               (do
                 (println (str "job-size " arg " is too big to fit in Java int,"
                               " so it won't work for Clojure dotimes"))
                 (usage 1))
               (== temp 0) *default-repetitions*
               :else temp))))
(def num-threads
     (let [arg (nth *command-line-args* 3)]
       (when (not (re-matches #"^\d+$" arg))
         (println "num-threads specified was " arg " but must be an integer")
         (usage 1))
       (let [temp (. Integer valueOf arg 10)]
         (if (== temp 0)
           *default-modified-pmap-num-threads*
           temp))))


(defn my-lazy-map
  [f coll]
  (lazy-seq
    (when-let [s (seq coll)]
      (cons (f (first s)) (my-lazy-map f (rest s))))))


;; modified-pmap does what I thought pmap should do -- run about n
;; instances of the function f at a time in parallel in separate
;; threads, and only when one of those is complete should another
;; instance be started.  I've changed map to my-lazy-map.  It seems
;; that the difference is that the version of map used by pmap
;; optimizes for chunked collections, so if coll is chunked, it will
;; "rush" ahead and start evaluating a whole chunk's worth of (future
;; (f %)) calls at a time.

;; Note: The number of threads run in parallel turns out to be n+1.
;; I'm still not sure why this is so.  In my tests, even n=0 causes 2
;; threads to be run in parallel, so we use the regular sequential map
;; if the num-threads parameter is 1.

(defn modified-pmap
  "Like pmap from Clojure 1.1, but with only as much parallelism as
  there are available processors.  Uses my-lazy-map instead of map
  from core.clj, since that version of map can use unwanted additional
  parallelism for chunked collections, like ranges."
  ([num-threads f coll]
     (if (== num-threads 1)
       (map f coll)
       (let [n (if (>= num-threads 2) (dec num-threads) 1)
             rets (my-lazy-map #(future (f %)) coll)
             step (fn step [[x & xs :as vs] fs]
                    (lazy-seq
                      (if-let [s (seq fs)]
                        (cons (deref x) (step xs (rest s)))
                        (map deref vs))))]
         (step rets (drop n rets)))))
  ([num-threads f coll & colls]
     (let [step (fn step [cs]
                  (lazy-seq
                    (let [ss (my-lazy-map seq cs)]
                      (when (every? identity ss)
                        (cons (my-lazy-map first ss) (step (my-lazy-map rest ss)))))))]
       (modified-pmap num-threads #(apply f %) (step (cons coll colls))))))


(defn spin-int [x]
  (let [reps job-size]
    (println (str "spin-int begin x=" x " reps=" reps))
    (dotimes [_ reps]
      (inc 0))))


(defn spin-double [x]
  (let [reps job-size]
    (println (str "spin-double begin x=" x " reps=" reps))
    (dotimes [_ reps]
;;      (inc (double 0.1))
      (inc 0.1))))


(defn maptest [n mapper fn & num-threads]
  (if (= mapper modified-pmap)
    (doall (mapper (first num-threads) fn (range n)))
    (doall (mapper fn (range n)))))


(def task-fn
     (condp = task-fn-specifier
       "int" spin-int
       "double" spin-double))

(let [p (.. Runtime getRuntime availableProcessors)]
  (println (str "availableProcessors=" p "  num-threads=" num-threads)))
(println)

(println (str "(maptest " num-jobs " modified-pmap " task-fn-specifier " " num-threads ")"))
(time (maptest num-jobs modified-pmap task-fn num-threads))

(. System (exit 0))
