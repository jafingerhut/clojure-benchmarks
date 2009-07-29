(with-open [br (java.io.BufferedReader. *in*)]
  (dorun (map #(println (format "%d %s" %1 %2))
	      (iterate inc 1)
	      (line-seq br))))
