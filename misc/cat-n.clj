(with-open [br (java.io.BufferedReader. *in*)]
  (dorun (map #(printf "%d %s\n" %1 %2)
	      (iterate inc 1)
	      (line-seq br))))
