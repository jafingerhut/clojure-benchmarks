(ns clj.benchmark.threadring (:gen-class))
 
(defn relay [state hops]
  (let [hops (dec (int hops)), nxt (:next state)]
    (if (neg? hops)
      (do (prn (:id state)) (shutdown-agents) state)
      (do (send nxt relay hops) state))))
 
(defn agent-ring [n]
  (let [tl (agent {:next nil :id n})
        hd (reduce (fn [next id] (agent {:next next :id id})) tl (reverse (range 1 n)))]
    (send tl #(assoc % :next hd)) ;; hook up head and tail
    (await tl)
    hd))
 
(defn run [n-agents hops]
  (let [hd (agent-ring n-agents)]
    (send hd relay hops)
    :done))
 
(defn -main [arg]
  (run 503 (Integer/parseInt arg)))
