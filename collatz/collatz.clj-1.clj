(ns collatz
  (:gen-class))

(set! *warn-on-reflection* true)


(defn cseq [n]
  (if (= 1 n)
    [1]
    (cons n (cseq (if (even? n)
                    (/ n 2)
                    (+ (* 3 n) 1 ))))))


(defn tracing-cseq [n]
  (if (zero? (mod n 10000))
  ;(if (zero? (mod n 1))
    (println "cseq " n))
  (cseq n))


(defn -main [& args]
  (let [maxn (. Integer valueOf (nth args 0) 10)]
    (println (apply max-key count (map tracing-cseq (range 1 maxn))))))
