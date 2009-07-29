;;(println (class *command-line-args*))
;;(println (count *command-line-args*))

(def prog-name (first *command-line-args*))
(def args (rest *command-line-args*))

;;(println (format "prog-name=:%s:" prog-name))
;;(println (class args))
;;(println (count args))

(when (not= (count args) 1)
  (println (format "usage: %s input-file" prog-name))
  (flush)
  (. System (exit 1))
  )

(let [input-fname (nth args 0)]
  (println (format "input file: %s" input-fname)))

(. System (exit 0))
