;;(set! *warn-on-reflection* true)

(def *default-repetitions* 250000000)

(defn usage [exit-code]
  (println (format "usage: %s num-jobs job-size" *file*))
  (println (format "    num-jobs and job-size must be positive integers, or job-size can be 0 to use the default number of repetitions: %d" *default-repetitions*))
  (. System (exit exit-code)))

(when (not= 2 (count *command-line-args*))
  (usage 1))
(when (not (re-matches #"^\d+$" (nth *command-line-args* 0)))
  (usage 1))
(def num-jobs (. Integer valueOf (nth *command-line-args* 0) 10))
(when (< num-jobs 1)
  (usage 1))
(when (not (re-matches #"^\d+$" (nth *command-line-args* 1)))
  (usage 1))
(def job-size
     (let [temp (. Integer valueOf (nth *command-line-args* 1) 10)]
       (if (== temp 0)
         *default-repetitions*
         temp)))


;; It turns out this doesn't do what I hoped for.
(defn modified-pmap1
  "Modified version of pmap from core.clj, which attempts to use
   delay/force to delay the start of the many threads.  For some
   reason I don't know, the original pmap seems to start up all
   threads in coll simultaneously when pmap is called, no matter how
   long the list is, even on my system where the number of available
   processors is 2, and thus n below will be 4."
  ([f coll]
   (let [n (+ 2 (.. Runtime getRuntime availableProcessors))
         rets (map #(delay (future (f %))) coll)
         step (fn step [[x & xs :as vs] fs]
                (lazy-seq
                 (if-let [s (seq fs)]
                   (cons (force (deref x)) (step xs (rest s)))
                   (map #(force (deref %)) vs))))]
     (step rets (drop n rets))))
  ([f coll & colls]
   (let [step (fn step [cs]
                (lazy-seq
                 (let [ss (map seq cs)]
                   (when (every? identity ss)
                     (cons (map first ss) (step (map rest ss)))))))]
     (pmap #(apply f %) (step (cons coll colls))))))


(defn my-lazy-map
  [f coll]
  (lazy-seq
    (when-let [s (seq coll)]
      (cons (f (first s)) (my-lazy-map f (rest s))))))


;; This does what I thought pmap should do -- run about n instances of
;; the functino f at a time in parallel in separate threads, and only
;; when one of those is complete should another instance be started.
;; The only thing I've changed is map -> my-lazy-map.  Could it be
;; that in core.clj, pmap is using a non-lazy version of map, and that
;; is why all threads are started right at the beginning?

(defn modified-pmap2
  "Like map, except f is applied in parallel. Semi-lazy in that the
  parallel computation stays ahead of the consumption, but doesn't
  realize the entire result unless required. Only useful for
  computationally intensive functions where the time of f dominates
  the coordination overhead."
  ([f coll]
   (let [n (+ 2 (.. Runtime getRuntime availableProcessors))
         rets (my-lazy-map #(future (f %)) coll)
         step (fn step [[x & xs :as vs] fs]
                (lazy-seq
                 (if-let [s (seq fs)]
                   (cons (deref x) (step xs (rest s)))
                   (my-lazy-map deref vs))))]
     (step rets (drop n rets))))
  ([f coll & colls]
   (let [step (fn step [cs]
                (lazy-seq
                 (let [ss (my-lazy-map seq cs)]
                   (when (every? identity ss)
                     (cons (my-lazy-map first ss) (step (my-lazy-map rest ss)))))))]
     (pmap #(apply f %) (step (cons coll colls))))))


(defn spin-int [x]
  (println (str "spin-int begin " x))
  (let [reps job-size]
    (dotimes [_ reps]
      (inc 0)))
  (println (str "spin-int end " x)))

(defn spin-double [x]
  (println (str "spin-double begin " x))
  (let [reps job-size]
    (dotimes [_ reps]
;;      (inc (double 0.1))
      (inc 0.1)
      ))
  (println (str "spin-double end " x)))

(defn maptest [n mapper fn]
  (doall (mapper fn (range n))))


(println (str "(maptest " num-jobs " modified-pmap2 spin-int):"))
(time (maptest num-jobs modified-pmap2 spin-int))
;(println (str "(maptest " num-jobs " modified-pmap1 spin-int):"))
;(time (maptest num-jobs modified-pmap1 spin-int))
(println (str "(maptest " num-jobs " map spin-int):"))
(time (maptest num-jobs map spin-int))
(println (str "(maptest " num-jobs " pmap spin-int):"))
(time (maptest num-jobs pmap spin-int))

;(println (str "(maptest " num-jobs " spin-double):"))
;(time (maptest num-jobs spin-double))
;(println (str "(pmaptest " num-jobs " spin-double):"))
;(time (pmaptest num-jobs spin-double))

(. System (exit 0))
