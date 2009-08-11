;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: Aug 10, 2009

(ns clojure.benchmark.n-body)

(set! *warn-on-reflection* true)

(defn usage [exit-code]
  (println (format "usage: %s n" *file*))
  (println (format "    n, a positive integer, is the number of simulation steps to run"))
  (. System (exit exit-code)))

(when (not= (count *command-line-args*) 1)
  (usage 1))
(def n
     (let [arg (nth *command-line-args* 0)]
       (when (not (re-matches #"^\d+$" arg))
         (usage 1))
       (let [temp (. Integer valueOf arg 10)]
         (when (< temp 1)
           (usage 1))
         temp)))


(defmacro mass [p] `(double (aget ~p 0)))
(defmacro posx [p] `(double (aget ~p 1)))
(defmacro posy [p] `(double (aget ~p 2)))
(defmacro posz [p] `(double (aget ~p 3)))
(defmacro velx [p] `(double (aget ~p 4)))
(defmacro vely [p] `(double (aget ~p 5)))
(defmacro velz [p] `(double (aget ~p 6)))

(defmacro set-mass! [p new-mass] `(aset ~p 0 ~new-mass))
(defmacro set-posx! [p new-posx] `(aset ~p 1 ~new-posx))
(defmacro set-posy! [p new-posy] `(aset ~p 2 ~new-posy))
(defmacro set-posz! [p new-posz] `(aset ~p 3 ~new-posz))
(defmacro set-velx! [p new-velx] `(aset ~p 4 ~new-velx))
(defmacro set-vely! [p new-vely] `(aset ~p 5 ~new-vely))
(defmacro set-velz! [p new-velz] `(aset ~p 6 ~new-velz))


(defn planet-construct [p]
  ;; Don't bother keeping the name around
  (let [p-arr (make-array Double/TYPE 7)]
    (set-mass! p-arr (:mass p))
    (set-posx! p-arr ((:pos p) 0))
    (set-posy! p-arr ((:pos p) 1))
    (set-posz! p-arr ((:pos p) 2))
    (set-velx! p-arr ((:velocity p) 0))
    (set-vely! p-arr ((:velocity p) 1))
    (set-velz! p-arr ((:velocity p) 2))
    p-arr))


(defn offset-momentum [bodies]
  (let [n (int (count bodies))]
    (loop [momx (double 0.0)
           momy (double 0.0)
           momz (double 0.0)
           i (int 0)]
      (if (< i n)
        (let [b (bodies i)
              m (mass b)]
          (recur (+ momx (* m (velx b)))
                 (+ momy (* m (vely b)))
                 (+ momz (* m (velz b)))
                 (unchecked-inc i)))
        [momx momy momz]))))


(defn n-body-system []
  (let [PI (double 3.141592653589793)
        SOLAR-MASS (double (* (double 4) PI PI))
        DAYS-PER-YEAR (double 365.24)
        bodies
        [ (planet-construct
           {:name "sun"
            :mass SOLAR-MASS
            :pos [0 0 0]
            :velocity [0 0 0]})
          (planet-construct
           {:name "jupiter"
            :mass (double (* 9.54791938424326609e-04 SOLAR-MASS))
            :pos [(double  4.84143144246472090e+00)
                  (double -1.16032004402742839e+00)
                  (double -1.03622044471123109e-01)]
            :velocity [(double (*  1.66007664274403694e-03 DAYS-PER-YEAR))
                       (double (*  7.69901118419740425e-03 DAYS-PER-YEAR))
                       (double (* -6.90460016972063023e-05 DAYS-PER-YEAR))]})
          (planet-construct
           {:name "saturn"
            :mass (double (* 2.85885980666130812e-04 SOLAR-MASS))
            :pos [(double  8.34336671824457987e+00)
                  (double  4.12479856412430479e+00)
                  (double -4.03523417114321381e-01)]
            :velocity [(double (* -2.76742510726862411e-03 DAYS-PER-YEAR))
                       (double (*  4.99852801234917238e-03 DAYS-PER-YEAR))
                       (double (*  2.30417297573763929e-05 DAYS-PER-YEAR))]})
          (planet-construct
           {:name "uranus"
            :mass (double (* 4.36624404335156298e-05 SOLAR-MASS))
            :pos [(double  1.28943695621391310e+01)
                  (double -1.51111514016986312e+01)
                  (double -2.23307578892655734e-01)]
            :velocity [(double (*  2.96460137564761618e-03 DAYS-PER-YEAR))
                       (double (*  2.37847173959480950e-03 DAYS-PER-YEAR))
                       (double (* -2.96589568540237556e-05 DAYS-PER-YEAR))]})
          (planet-construct
           {:name "neptune"
            :mass (double (* 5.15138902046611451e-05 SOLAR-MASS))
            :pos [(double  1.53796971148509165e+01)
                  (double -2.59193146099879641e+01)
                  (double  1.79258772950371181e-01)]
            :velocity [(double (*  2.68067772490389322e-03 DAYS-PER-YEAR))
                       (double (*  1.62824170038242295e-03 DAYS-PER-YEAR))
                       (double (* -9.51592254519715870e-05 DAYS-PER-YEAR))]})
          ]]
    (let [[momx momy momz] (offset-momentum bodies)
          a (double (/ -1.0 SOLAR-MASS))
          sun-index 0
          sun (bodies sun-index)]
      (set-velx! sun (* a momx))
      (set-vely! sun (* a momy))
      (set-velz! sun (* a momz))
      (assoc bodies sun-index sun))))


(defn kinetic-energy-1 [body]
  (* (double 0.5) (mass body)
     (+ (* (velx body) (velx body))
        (* (vely body) (vely body))
        (* (velz body) (velz body)))))


(defn kinetic-energy [bodies]
  (reduce + (map kinetic-energy-1 bodies)))


(defn distance-between [b1 b2]
  (let [dx (double (- (posx b1) (posx b2)))
        dy (double (- (posy b1) (posy b2)))
        dz (double (- (posz b1) (posz b2)))]
    (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))))


(defn all-seq-ordered-pairs [s]
  (loop [s1 (seq s)
         pairs ()]
    (if s1
      (let [s1item (first s1)]
        (recur (next s1)
               (into pairs (map (fn [s2item] [s1item s2item]) (next s1)))))
      pairs)))


(defn potential-energy-body-pair [[b1 b2]]
  (let [distance (distance-between b1 b2)]
    (/ (* (mass b1) (mass b2))
       distance)))


(defn potential-energy [bodies]
  (- (reduce + (map potential-energy-body-pair
                    (all-seq-ordered-pairs bodies)))))


(defn energy [bodies]
  (+ (kinetic-energy bodies) (potential-energy bodies)))


(defmacro add-to-vel! [#^doubles body delta-vx delta-vy delta-vz]
  `(do
     (set-velx! ~body (+ (velx ~body) ~delta-vx))
     (set-vely! ~body (+ (vely ~body) ~delta-vy))
     (set-velz! ~body (+ (velz ~body) ~delta-vz))))


(defn bodies-update-velocities! [bodies delta-t]
  (let [n (int (count bodies))
        n-1 (int (dec n))
        delta-t (double delta-t)]
    (loop [i1 (int 0)]
      (if (< i1 n-1)
        (let [#^doubles b1 (bodies i1)]
          (loop [i2 (int (inc i1))]
            (if (< i2 n)
              (let [#^doubles b2 (bodies i2)
                    delta-posx (- (posx b1) (posx b2))
                    delta-posy (- (posy b1) (posy b2))
                    delta-posz (- (posz b1) (posz b2))
                    dist-squared (+ (+ (* delta-posx delta-posx)
                                       (* delta-posy delta-posy))
                                    (* delta-posz delta-posz))
                    dist (Math/sqrt dist-squared)
                    mag (/ (/ delta-t dist-squared) dist)
                    b1-scale (* (- mag) (mass b2))
                    dv1x (* delta-posx b1-scale)
                    dv1y (* delta-posy b1-scale)
                    dv1z (* delta-posz b1-scale)
                    b2-scale (* mag (mass b1))
                    dv2x (* delta-posx b2-scale)
                    dv2y (* delta-posy b2-scale)
                    dv2z (* delta-posz b2-scale)]
                (add-to-vel! b1 dv1x dv1y dv1z)
                (add-to-vel! b2 dv2x dv2y dv2z)
                (recur (unchecked-inc i2)))))
          (recur (unchecked-inc i1)))))))


(defn bodies-update-positions! [bodies delta-t]
  (let [n (int (count bodies))
        delta-t (double delta-t)]
    (loop [i (int 0)]
      (if (< i n)
        (let [#^doubles b (bodies i)]
          (set-posx! b (+ (posx b) (* (velx b) delta-t)))
          (set-posy! b (+ (posy b) (* (vely b) delta-t)))
          (set-posz! b (+ (posz b) (* (velz b) delta-t)))
          (recur (unchecked-inc i)))))))


(defn advance! [bodies delta-t]
  (bodies-update-velocities! bodies delta-t)
  (bodies-update-positions! bodies delta-t))


(let [bodies (n-body-system)
      delta-t (double 0.01)]
  (println (format "%.9f" (energy bodies)))
  (loop [i (int n)]
    (if (zero? i)
      (println (format "%.9f" (energy bodies)))
      (do
        (advance! bodies delta-t)
        (recur (unchecked-dec i))))))
