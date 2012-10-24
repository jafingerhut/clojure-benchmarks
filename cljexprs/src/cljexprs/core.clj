(ns cljexprs.core
  (:require [criterium.core :as criterium]
            [clojure.pprint :as pp]))


(defmacro benchmark
  [bindings expr & opts]
  `(do
     (printf "\n")
     (printf "----------------------------------------\n")
     (printf "Benchmarking %s\n" '~expr)
     (printf "with bindings %s\n" '~bindings)
     (printf "\n")
     (flush)
     ;(criterium/bench ~expr ~@opts)
     ;(pp/pprint (criterium/benchmark ~expr ~@opts))
     (let ~bindings
       (pp/pprint {:bindings '~bindings
                   :expr '~expr
                   :opts '~opts
                   :results (criterium/benchmark ~expr ~@opts)}))
     ;(criterium/quick-bench ~expr ~@opts)

     (flush)))


(defn -main [& args]
  (println "Hello, World!")

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
  
  (println "Goodbye, World!"))
