(let [input-fname "cat-n.clj"]
  (with-open [fr (java.io.FileReader. input-fname)
	      br (java.io.BufferedReader. fr)]
    (dorun (map #(println (format "%d %s" %1 %2))
		(iterate inc 1)
		(line-seq br)))))
