(ns pmapalternates
  (:use [clojure.string :only (join)])
  (:import java.util.concurrent.ExecutorService
           java.util.concurrent.Executors)
  (:gen-class))

(set! *warn-on-reflection* true)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This is a copy of part of Amit Rathore's Medusa package, which
;; allows you to submit a bunch of Clojure expressions to run to a
;; thread pool with a fixed size.  No more than that many threads will
;; ever run at once, but Medusa tries to keep that many threads going
;; at all times, as long as there are things to do that have been
;; submitted.
;;
;; git clone http://github.com/amitrathore/clj-utils.git
;; git clone http://github.com/amitrathore/medusa.git
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def THREADPOOL)

(def running-futures (ref {}))

(defn create-runonce [function]
  (let [sentinel (Object.)
        result (atom sentinel)] 
    (fn [& args]
      (locking sentinel 
        (if (= @result sentinel)
          (reset! result (apply function args)) 
          @result)))))

(defmacro defrunonce [fn-name args & body]
  `(def ~fn-name (create-runonce (fn ~args ~@body))))

(defn new-fixed-threadpool [size]
  (Executors/newFixedThreadPool size))

(defrunonce init-medusa [pool-size]
  (def THREADPOOL (new-fixed-threadpool pool-size)))

(defn claim-thread [future-id]
  (let [thread-info {:thread (Thread/currentThread) :future-id future-id
                     :started (System/currentTimeMillis)}]
    (dosync (alter running-futures assoc future-id thread-info))))

(defn mark-completion [future-id]
  (dosync (alter running-futures dissoc future-id)))

(defn medusa-future-thunk [future-id thunk]
  (let [work (fn []
               (claim-thread future-id)
               (let [val (thunk)]
                 (mark-completion future-id)
                 val))]
    (.submit THREADPOOL work)))

(defn random-uuid []
  (str (java.util.UUID/randomUUID)))

(defmacro medusa-future [& body]
  `(medusa-future-thunk (random-uuid) (fn [] (do ~@body))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This is the end of the subset of Medusa code.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; This is my attempt to write a function like pmap, except using
;; Medusa as a basis.  Note that it does not return a useful value,
;; the way pmap does.  Perhaps I can fix this in the future.

(defn medusa-pmap [num-threads f coll]
  (if (== num-threads 1)
    (map f coll)
    (do
      (init-medusa num-threads)
      (let [seq-of-futures (doall (map #(medusa-future (f %)) coll))]
        (map (fn [java-future] (.get java-future)) seq-of-futures)))))


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
  specified by arg num-threads.  Uses my-lazy-map instead of map
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
                        (cons (my-lazy-map first ss)
                              (step (my-lazy-map rest ss)))))))]
       (modified-pmap num-threads #(apply f %) (step (cons coll colls))))))


(defn spin-int [x job-size]
  (let [reps job-size]
    (printf "spin-int begin x=%d reps=%d\n" x reps)
    (dotimes [_ reps]
      (inc 0))))


(defn spin-long [x job-size]
  (let [reps job-size]
    (printf "spin-long begin x=%d reps=%d\n" x reps)
    (dotimes [_ reps]
      (inc (long 0)))))


(defn spin-float-primitive [x job-size]
  (let [reps job-size]
    (printf "spin-float-primitive begin x=%d reps=%d\n" x reps)
    (dotimes [_ reps]
      (inc (float 0.1)))))


(defn spin-double [x job-size]
  (let [reps job-size]
    (printf "spin-double begin x=%d reps=%d\n" x reps)
    (dotimes [_ reps]
      (inc 0.1))))


(defn spin-double1 [x job-size]
  (let [reps (long job-size)]
    (printf "spin-double1 begin x=%d reps=%d\n" x reps)
    (println (str
              (loop [i (long 0)]
                (when (< i reps)
                  (inc 0.1)
                  (recur (unchecked-inc i))))))))


(defn spin-double2 [x job-size]
  (let [reps (long job-size)]
    (printf "spin-double2 begin x=%d reps=%d\n" x reps)
    (println (str
              (loop [i (long 0)
                     c (double 0.0)]
                (if (< i reps)
                  (recur (unchecked-inc i) (inc c))
                  c))))))


(defn spin-double-primitive [x job-size]
  (let [reps job-size]
    (printf "spin-double-primitive begin x=%d reps=%d\n" x reps)
    (dotimes [_ reps]
      (inc (double 0.1)))))


(defmacro dotimes-long
  "bindings => name n

  Repeatedly executes body (presumably for side-effects) with name
  bound to long integers from 0 through n-1."
  {:added "1.0"}
  [bindings & body]
  (let [i (first bindings)
        n (second bindings)]
    `(let [n# (long ~n)]
       (loop [~i (long 0)]
         (when (< ~i n#)
           ~@body
           (recur (inc ~i)))))))


(defn var-len-jobs1 [x job-size]
  (let [reps (if (== x 1) (* 10 job-size) job-size)]
    (printf "var-len-jobs1 begin x=%d reps=%d\n" x reps)
    (flush)
    (dotimes-long [_ reps]
      (inc 0))
    (printf "var-len-jobs1 end   x=%d reps=%d\n" x reps)
    (flush)
    (format "(ret val of var-len-jobs1 %d)" x)))


(defn maptest [n mapper fn & num-threads]
  (if (= mapper modified-pmap)
    (doall (mapper (first num-threads) fn (range n)))
    (doall (mapper fn (range n)))))



(def *default-repetitions* 1000000000)
(def *default-modified-pmap-num-threads*
     (+ 2 (.. Runtime getRuntime availableProcessors)))

(def *allowed-types* ["int" "long" "float-primitive"
                      "double" "double1" "double2" "double-primitive"
                      "pmap-var-len-jobs1" "medusa-var-len-jobs1"])

(defn usage [exit-code]
  (printf "usage: %s type num-jobs job-size num-threads\n" *file*)
  (printf "    type must be one of: %s\n" (join "," *allowed-types*))
  (printf "    all other arguments must be integers >= 0\n")
  (printf "    num-jobs must be >= 1, and is the number of jobs in the list to perform\n")
  (printf "    job-size is the number of steps in each job\n")
  (printf "        0 means to use the default number of steps: %d\n"
          *default-repetitions*)
  (printf "    num-threads is the number of threads to run in parallel\n")
  (printf "        0 means to use the default number of threads: %d\n"
          *default-modified-pmap-num-threads*)
  (printf "        1 means to use sequential map, guaranteeing no parallelism\n")
  (flush)
  (. System (exit exit-code)))


(defn -main [& args]
  (when (not= 4 (count args))
    (printf "Expected 4 args but found %d\n" (count args))
    (usage 1))
  (let [task-fn-specifier
        (let [arg (nth args 0)
              temp ((into #{} *allowed-types*) arg)]
          (if temp
            temp
            (do
              (printf "type specified was %s but must be one of: %s\n"
                      arg (join "," *allowed-types*))
              (usage 1))))
        num-jobs
        (let [arg (nth args 1)]
          (when (not (re-matches #"^\d+$" arg))
            (printf "num-jobs specified was %s but must be an integer\n" arg)
            (usage 1))
          (let [temp (. Integer valueOf arg 10)]
            (when (< temp 1)
              (usage 1))
            temp))
        job-size
        (let [arg (nth args 2)]
          (when (not (re-matches #"^\d+$" arg))
            (printf "job-size specified was %s but must be an integer\n" arg)
            (usage 1))
          (let [temp (BigInteger. #^String arg)]
            (cond
;;             (not= temp (int temp))
;;             (do
;;               (printf "job-size %s is too big to fit in Java int, so it won't work for Clojure dotimes\n" arg)
;;               (usage 1))
             (== temp 0) *default-repetitions*
             :else temp)))
        num-threads
        (let [arg (nth args 3)]
          (when (not (re-matches #"^\d+$" arg))
            (printf "num-threads specified was %s but must be an integer\n" arg)
            (usage 1))
          (let [temp (. Integer valueOf arg 10)]
            (if (== temp 0)
              *default-modified-pmap-num-threads*
              temp)))
        task-fn
        (condp = task-fn-specifier
            "int" spin-int
            "long" spin-long
            "float-primitive" spin-float-primitive
            "double" spin-double
            "double1" spin-double1
            "double2" spin-double2
            "double-primitive" spin-double-primitive
            "pmap-var-len-jobs1" var-len-jobs1
            "medusa-var-len-jobs1" var-len-jobs1
            )
        p (.. Runtime getRuntime availableProcessors)]
    (printf "availableProcessors=%d  num-threads=%d\n" p num-threads)
    (printf "Integer.SIZE=%d bits\n" Integer/SIZE)
    (printf "Long.SIZE=%d bits\n" Long/SIZE)
    (printf "Float.SIZE=%d bits\n" Float/SIZE)
    (printf "Double.SIZE=%d bits\n" Double/SIZE)
    (printf "\n")
  
;;    (printf "(maptest %d modified-pmap %s %d)\n"
;;            num-jobs task-fn-specifier num-threads)
;;  (time (maptest num-jobs modified-pmap task-fn num-threads))

    (if (contains? {"medusa-var-len-jobs1" nil} task-fn-specifier)
      (do
        (printf "(medusa-pmap %d %s (range %d))\n"
                num-threads task-fn-specifier num-jobs)
        (println (time (doall (medusa-pmap num-threads #(task-fn % job-size)
                                           (range num-jobs)))))
        ;;(Thread/sleep (* 30 1000))
        )
      (do
        (printf "(modified-pmap %d %s (range %d))\n"
                num-threads task-fn-specifier num-jobs)
        (time (doall (modified-pmap num-threads #(task-fn % job-size)
                                    (range num-jobs))))))
    (flush)
    (. System (exit 0))))
