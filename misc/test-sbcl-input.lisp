(defmacro while (test &body body)
  `(do ()
       ((not ,test))
     ,@body))

(defconstant +ub+ '(unsigned-byte 8))

;;(with-open-file (in "in.txt")
;;(with-open-file (in "/dev/stdin")
(let ((in *standard-input*))
  (let (ln
	(count 0))
    (while (not (eql :eof (setf ln (read-line in nil :eof))))
      (incf count))
    (format t "Read ~D lines.~%" count)))

(quit)
