;;(set! *warn-on-reflection* true)

(def *default-repetitions* 250000000)
(def *default-modified-pmap-num-threads*
     (+ 2 (.. Runtime getRuntime availableProcessors)))

(defn usage [exit-code]
  (println (format "usage: %s num-jobs job-size num-threads" *file*))
  (println (format "    all arguments must be integers >= 0"))
  (println (format "    num-jobs must be >= 1, and is the number of jobs in the list to perform"))
  (println (format "    job-size is the number of steps in each job"))
  (println (format "        0 means to use the default number of steps: %d"
                   *default-repetitions*))
  (println (format "    num-threads is the number of threads to run in parallel for the modified-pmap part of the test"))
  (println (format "        0 means to use the default number of threads: %d"
                   *default-modified-pmap-num-threads*))
  (. System (exit exit-code)))

(when (not= 3 (count *command-line-args*))
  (usage 1))

(when (not (re-matches #"^\d+$" (nth *command-line-args* 0)))
  (usage 1))
(def num-jobs (. Integer valueOf (nth *command-line-args* 0) 10))
(when (< num-jobs 1)
  (usage 1))

(when (not (re-matches #"^\d+$" (nth *command-line-args* 1)))
  (usage 1))
(def job-size
     (let [temp (BigInteger. (nth *command-line-args* 1))]
       (cond (not= temp (inc temp)) (do
                                      (println (str "job-size " (nth *command-line-args* 1) " is too big to fit in Java int, so it won't work for Clojure dotimes"))
                                      (usage 1))
             (== temp 0) *default-repetitions*
             :else temp)))

(when (not (re-matches #"^\d+$" (nth *command-line-args* 2)))
  (usage 1))
(def num-threads
     (let [temp (. Integer valueOf (nth *command-line-args* 2) 10)]
       (if (== temp 0)
         *default-modified-pmap-num-threads*
         temp)))


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
;  (println (str "spin-int begin " x))
  (let [reps job-size]
    (println (str "spin-int begin reps=" reps))
    (dotimes [_ reps]
      (inc 0)))
;  (println (str "spin-int end " x))
  )


(defn spin-double [x]
;  (println (str "spin-double begin " x))
  (let [reps job-size]
    (dotimes [_ reps]
;;      (inc (double 0.1))
      (inc 0.1)
      ))
;  (println (str "spin-double end " x))
  )


(defn maptest [n mapper fn & num-threads]
  (if (= mapper modified-pmap)
    (doall (mapper (first num-threads) fn (range n)))
    (doall (mapper fn (range n)))))


(let [p (.. Runtime getRuntime availableProcessors)]
  (println (str "availableProcessors=" p "  parallelism used by pmap=" (+ 2 p))))
(println (str "parallelism requested for modified-pmap=" num-threads
              " threads"))
(println)

(println (str "(maptest " num-jobs " modified-pmap spin-int):"))
(time (maptest num-jobs modified-pmap spin-int num-threads))
;;(println (str "(maptest " num-jobs " map spin-int):"))
;;(time (maptest num-jobs map spin-int))
;;(println (str "(maptest " num-jobs " pmap spin-int):"))
;;(time (maptest num-jobs pmap spin-int))

;(println (str "(maptest " num-jobs " spin-double):"))
;(time (maptest num-jobs spin-double))
;(println (str "(pmaptest " num-jobs " spin-double):"))
;(time (pmaptest num-jobs spin-double))

(. System (exit 0))
