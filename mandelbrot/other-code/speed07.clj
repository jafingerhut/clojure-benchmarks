;; William D. Lipe
;; atmcsld@gmail.com
;; April 1, 2009
;; http://groups.google.com/group/clojure/tree/browse_frm/month/2009-04/22e5cbfda1c844d7?rnum=81&_done=%2Fgroup%2Fclojure%2Fbrowse_frm%2Fmonth%2F2009-04%3F#doc_1c065703afa83dd1


(defn check-bounds [x y]
 (let [px (float x) py (float y)]
   (loop [zx (float 0.0)
          zy (float 0.0)
          zx2 (float 0.0)
          zy2 (float 0.0)
          value (int 0)]
      (if (and (< value *max-steps*) (< (+ zx2 zy2) 4.0))
           (let [new-zy (float (+ (* 2.0 zx zy) py))
                 new-zx (float (+ (- zx2 zy2) px))
                 new-zx2 (float (* new-zx new-zx))
                 new-zy2 (float (* new-zy new-zy))]
                 (recur new-zx new-zy new-zx2 new-zy2 (inc value)))
           (if (== value *max-steps*) 0 value)))))


(defn draw [#^Canvas canvas]
 (let [#^BufferStrategy buffer (. canvas getBufferStrategy)
	#^Graphics       g (. buffer getDrawGraphics)]
   (doseq [y (range 0 *height*)]
     (let [dy (- 1.5 (* 2.5 (/ y *height*)))]
	(doseq [x (range 0 *width*)]
	  (let [dx (- (* 3 (/ x *width*)) 2.1)
		value (check-bounds dx dy)]
	    (when (> value 0)
	      (.setColor g (Color. (* value (/ 255 *max-steps*))))
	      (.drawRect g x y 0 0))))
	(.show buffer)))
   (.show buffer)))
