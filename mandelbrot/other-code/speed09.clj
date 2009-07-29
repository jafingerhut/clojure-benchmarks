;; michael.messinides@invista.com
;; April 2, 2009
;; http://groups.google.com/group/clojure/tree/browse_frm/month/2009-04/553459b6f69083ad?rnum=91&_done=%2Fgroup%2Fclojure%2Fbrowse_frm%2Fmonth%2F2009-04%3F#doc_553459b6f69083ad

defn check-bounds [x y]
  (let [f2 (float 2.0)
        f4 (float 4.0)]
  (loop [px (float x)
         py (float y)
         zx (float 0.0)
         zy (float 0.0)
         zx2 (float 0.0)
         zy2 (float 0.0)
         value (float 0)]
     (if (and (< value (*max-steps*)) (< (+ zx2 zy2) f4))
          (let [new-zy (float (+ (* (* f2 zx) zy) py))
                new-zx (float (+ (- zx2 zy2) px))
                new-zx2 (float (* new-zx new-zx))
                new-zy2 (float (* new-zy new-zy))]
                (recur px py new-zx new-zy new-zx2 new-zy2 (inc
value)))
          (if (== value (*max-steps*)) 0 value)))))

;; f2 and f4 didn't do much, most improvement seems to come from (* (* f2
;; zx) zy) in place of (* f2 zx zy). Using the arity 2 multiply results
;; in the multiply being inlined.
