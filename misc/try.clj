;;(println (class *command-line-args*))
;;(println (count *command-line-args*))

(def prog-name (first *command-line-args*))
(def args (rest *command-line-args*))

;;(printf "prog-name=:%s:\n" prog-name)
;;(println (class args))
;;(println (count args))

(when (not= (count args) 1)
  (printf "usage: %s input-file\n" prog-name)
  (flush)
  (. System (exit 1))
  )

(let [input-fname (nth args 0)]
  (printf "input file: %s\n" input-fname))

(. System (exit 0))
