;; dmitri.sotnikov@gmail.com
;; April 1, 2009
;; http://groups.google.com/group/clojure/tree/browse_frm/month/2009-04/22e5cbfda1c844d7?rnum=81&_done=%2Fgroup%2Fclojure%2Fbrowse_frm%2Fmonth%2F2009-04%3F#doc_399c868e01556a04

(ns main
 (:import (java.awt Color Container Graphics Canvas Dimension)
          (javax.swing JPanel JFrame)
          (java.awt.image BufferedImage BufferStrategy)))

(def *width* 640)
(def *height* 640)
(def *max-steps* 32)

(defn on-thread [f] (doto (new Thread f) (.start)))

(defn check-bounds [x y]
   (loop [px x
          py y
          zx 0.0
          zy 0.0
          zx2 0.0
          zy2 0.0
          value 0]
      (if (and (< value *max-steps*) (< (+ zx2 zy2)
4.0))
           (let [new-zy (+ (* 2.0 zx zy) py)
                 new-zx (+ (- zx2 zy2) px)
                 new-zx2 (* new-zx new-zx)
                 new-zy2 (* new-zy new-zy)]
                 (recur px py new-zx new-zy new-zx2 new-zy2 (inc
value)))
           (if (== value *max-steps*) 0 value))))

(defn draw-line [g y]
   (let [dy (- 1.25 (* 2.5 (/ y *height*)))]
     (doseq [x (range 0 *width*)]
       (let [dx (- (* 2.5 (/ x *width*)) 2.0)]
               (let [value (check-bounds dx dy)]
                   (if (> value  0)
                       (doto g
                           (. setColor (Color. (* value (/ 255 *max-
steps*))))
                           (. drawRect x y 0 0))))))))

(defn draw-lines
   ([buffer g] (draw-lines buffer g *height*))
   ([buffer g y]
         (doseq [y (range 0 y)]
            (draw-line g y)
            ;(on-thread (draw-line g y))
            (. buffer show))))


(defn draw [canvas]
   (let [buffer (. canvas getBufferStrategy)
         g        (. buffer getDrawGraphics)]
         (draw-lines buffer g)))

(defn main []

 (let [panel (JPanel.)
       canvas (Canvas.)
       frame (JFrame. "Mandelbrot")]

   (doto panel
     (.setPreferredSize (Dimension. *width* *height*))
     (.setLayout nil)
     (.add canvas))

   (doto frame
     (.setDefaultCloseOperation JFrame/
EXIT_ON_CLOSE)
     (.setBounds 0,0,*width* *height*)
     (.setResizable false)
     (.add panel)
     (.setVisible true))

   (doto canvas
     (.setBounds 0,0,*width* *height*)
     (.setBackground (Color/BLACK))
     (.createBufferStrategy 2)
     (.requestFocus))

   (draw canvas)))

(time (main))
