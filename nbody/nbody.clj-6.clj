;; Author: Andy Fingerhut (andy_fingerhut@alum.wustl.edu)
;; Date: Aug 10, 2009

(ns nbody
  (:gen-class))

(set! *warn-on-reflection* true)


(defn vec-construct [x y z]
  [(double x) (double y) (double z)])


(defmacro vec-add [v1 v2]
  `(let [v1x# (double (~v1 0))
         v1y# (double (~v1 1))
         v1z# (double (~v1 2))
         v2x# (double (~v2 0))
         v2y# (double (~v2 1))
         v2z# (double (~v2 2))]
     [(+ v1x# v2x#)
      (+ v1y# v2y#)
      (+ v1z# v2z#)]))


(defmacro vec-sub [v1 v2]
  `(let [v1x# (double (~v1 0))
         v1y# (double (~v1 1))
         v1z# (double (~v1 2))
         v2x# (double (~v2 0))
         v2y# (double (~v2 1))
         v2z# (double (~v2 2))]
     [(- v1x# v2x#)
      (- v1y# v2y#)
      (- v1z# v2z#)]))


(defmacro vec-times-scalar [v scalar]
  `(let [vx# (double (~v 0))
         vy# (double (~v 1))
         vz# (double (~v 2))
         scalar# (double ~scalar)]
     [(* scalar# vx#)
      (* scalar# vy#)
      (* scalar# vz#)]))


(defmacro vec-dot-product [v1 v2]
  `(let [v1x# (double (~v1 0))
         v1y# (double (~v1 1))
         v1z# (double (~v1 2))
         v2x# (double (~v2 0))
         v2y# (double (~v2 1))
         v2z# (double (~v2 2))]
     (+ (* v1x# v2x#)
        (* v1y# v2y#)
        (* v1z# v2z#))))


(defn planet-construct [p]
  [(:name p) (:mass p) (:pos p) (:velocity p)])

(defmacro planet-mass [p]     `(~p 1))
(defmacro planet-pos [p]      `(~p 2))
(def +PLANET-VELOCITY-INDEX+ 3)
(defmacro planet-velocity [p] `(~p 3))


(defn offset-momentum [bodies]
  (let [n (int (count bodies))]
    (loop [momentum-vec (vec-times-scalar (planet-velocity (bodies 0))
                                          (planet-mass (bodies 0)))
           i (int 1)]
      (if (< i n)
        (let [b (bodies i)]
          (recur (vec-add momentum-vec
                          (vec-times-scalar (planet-velocity b)
                                            (planet-mass b)))
                 (unchecked-inc i)))
        momentum-vec))))


(defn n-body-system []
  (let [PI (double 3.141592653589793)
        SOLAR-MASS (double (* (double 4) PI PI))
        DAYS-PER-YEAR (double 365.24)
        bodies
        [ (planet-construct
           {:name "sun"
            :mass SOLAR-MASS
            :pos (vec-construct 0 0 0)
            :velocity (vec-construct 0 0 0)})
          (planet-construct
           {:name "jupiter"
            :mass (double (* 9.54791938424326609e-04 SOLAR-MASS))
            :pos (vec-construct (double  4.84143144246472090e+00)
                                (double -1.16032004402742839e+00)
                                (double -1.03622044471123109e-01))
            :velocity (vec-construct
                       (double (*  1.66007664274403694e-03 DAYS-PER-YEAR))
                       (double (*  7.69901118419740425e-03 DAYS-PER-YEAR))
                       (double (* -6.90460016972063023e-05 DAYS-PER-YEAR)))})
          (planet-construct
           {:name "saturn"
            :mass (double (* 2.85885980666130812e-04 SOLAR-MASS))
            :pos (vec-construct (double  8.34336671824457987e+00)
                                (double  4.12479856412430479e+00)
                                (double -4.03523417114321381e-01))
            :velocity (vec-construct
                       (double (* -2.76742510726862411e-03 DAYS-PER-YEAR))
                       (double (*  4.99852801234917238e-03 DAYS-PER-YEAR))
                       (double (*  2.30417297573763929e-05 DAYS-PER-YEAR)))})
          (planet-construct
           {:name "uranus"
            :mass (double (* 4.36624404335156298e-05 SOLAR-MASS))
            :pos (vec-construct (double  1.28943695621391310e+01)
                                (double -1.51111514016986312e+01)
                                (double -2.23307578892655734e-01))
            :velocity (vec-construct
                       (double (*  2.96460137564761618e-03 DAYS-PER-YEAR))
                       (double (*  2.37847173959480950e-03 DAYS-PER-YEAR))
                       (double (* -2.96589568540237556e-05 DAYS-PER-YEAR)))})
          (planet-construct
           {:name "neptune"
            :mass (double (* 5.15138902046611451e-05 SOLAR-MASS))
            :pos (vec-construct (double  1.53796971148509165e+01)
                                (double -2.59193146099879641e+01)
                                (double  1.79258772950371181e-01))
            :velocity (vec-construct
                       (double (*  2.68067772490389322e-03 DAYS-PER-YEAR))
                       (double (*  1.62824170038242295e-03 DAYS-PER-YEAR))
                       (double (* -9.51592254519715870e-05 DAYS-PER-YEAR)))})
          ]]
    (let [sun-index 0
          init-sun-velocity (vec-times-scalar (offset-momentum bodies)
                                              (double (/ -1.0 SOLAR-MASS)))]
      (assoc-in bodies [sun-index +PLANET-VELOCITY-INDEX+] init-sun-velocity))))


(defn kinetic-energy-1 [mass vel]
  (* (double 0.5) mass
     (vec-dot-product vel vel)))


(defn kinetic-energy [body-masses body-velocities]
;;  (doall
;;   (for [i (range (count body-masses))]
;;     (printf "i=%d body[i] kinetic energy=%.9f\n"
;;             i (kinetic-energy-1 (body-masses i) (body-velocities i)))))
  (reduce + (map kinetic-energy-1 body-masses body-velocities)))


(defn distance-between [pos1 pos2]
  (let [delta-pos (vec-sub pos1 pos2)]
    (Math/sqrt (vec-dot-product delta-pos delta-pos))))


(defn all-seq-ordered-pairs [s]
  (loop [s1 (seq s)
         pairs ()]
    (if s1
      (let [s1item (first s1)]
        (recur (next s1)
               (into pairs (map (fn [s2item] [s1item s2item]) (next s1)))))
      pairs)))


(defn potential-energy-body-pair [[[b1-mass b1-pos] [b2-mass b2-pos]]]
  (let [distance (distance-between b1-pos b2-pos)]
    (/ (* b1-mass b2-mass)
       distance)))


(defn potential-energy [body-masses body-positions]
  (- (reduce + (map potential-energy-body-pair
                    (all-seq-ordered-pairs (map (fn [x y] [x y])
                                                body-masses
                                                body-positions))))))


(defn energy [body-masses body-positions body-velocities]
;;  (printf "kinetic-energy: %.9f\n"
;;          (kinetic-energy body-masses body-velocities))
;;  (printf "potential-energy: %.9f\n" 
;;          (potential-energy body-masses body-positions))
  (+ (kinetic-energy body-masses body-velocities)
     (potential-energy body-masses body-positions)))


(defn delta-velocities-for-body-pair [b1-mass b1-pos b2-mass b2-pos delta-t]
  (let [delta-pos (vec-sub b1-pos b2-pos)
        dist-squared (double (vec-dot-product delta-pos delta-pos))
        dist (double (Math/sqrt dist-squared))
        mag (double (/ delta-t dist-squared dist))
        b1-delta-v (vec-times-scalar delta-pos (* (double -1.0) mag (double b2-mass)))
        b2-delta-v (vec-times-scalar delta-pos (* mag (double b1-mass)))]
    [ b1-delta-v b2-delta-v ]))


(defn bodies-new-velocities [body-masses body-positions body-velocities
                             delta-t]
;;  (println (str "bodies-new-velocities: " (count body-masses)
;;                " " (count body-positions)
;;                " " (count body-velocities)))
  (let [n (int (count body-masses))
        n-1 (int (dec n))
        new-velocities (transient body-velocities)]
    (loop [i1 (int 0)]
      (if (< i1 n-1)
        (do
          (loop [i2 (int (inc i1))]
            (if (< i2 n)
              (let [[delta-v1 delta-v2]
                    (delta-velocities-for-body-pair
                     (body-masses i1) (body-positions i1)
                     (body-masses i2) (body-positions i2) delta-t)]
                (assoc! new-velocities i1
                        (vec-add (new-velocities i1) delta-v1))
                (assoc! new-velocities i2
                        (vec-add (new-velocities i2) delta-v2))
                (recur (unchecked-inc i2)))))
          (recur (unchecked-inc i1)))
        ;; else (== i1 n-1)
        (persistent! new-velocities)))))


(defn bodies-new-positions [body-positions body-velocities delta-t]
  (let [n (int (count body-positions))
        new-positions (transient [])
        delta-t (double delta-t)]
    (loop [i (int 0)
           new-positions (transient [])]
      (if (< i n)
        (recur (unchecked-inc i)
               (conj! new-positions
                      (vec-add (body-positions i)
                               (vec-times-scalar (body-velocities i) delta-t))))
        (persistent! new-positions)))))


(defn advance [body-masses body-positions body-velocities delta-t]
  (let [new-velocities
        (bodies-new-velocities body-masses body-positions body-velocities
                               delta-t)]
    [new-velocities
     (bodies-new-positions body-positions new-velocities delta-t)]))


(defn vec-of-masses [bodies] (vec (map (fn [b] (planet-mass b)) bodies)))
(defn vec-of-positions [bodies] (vec (map (fn [b] (planet-pos b)) bodies)))
(defn vec-of-velocities [bodies] (vec (map (fn [b] (planet-velocity b)) bodies)))


(defn usage [exit-code]
  (printf "usage: %s n\n" *file*)
  (printf "    n, a positive integer, is the number of simulation steps to run\n")
  (flush)
  (. System (exit exit-code)))


(defn -main [& args]
  (when (not= (count args) 1)
    (usage 1))
  (def n
       (let [arg (nth args 0)]
         (when (not (re-matches #"^\d+$" arg))
           (usage 1))
         (let [temp (. Integer valueOf arg 10)]
           (when (< temp 1)
             (usage 1))
           temp)))
  (let [bodies (n-body-system)
        planet-masses (vec-of-masses bodies)
        planet-positions (vec-of-positions bodies)
        planet-velocities (vec-of-velocities bodies)
        delta-t (double 0.01)
        all-ordered-body-index-pairs (all-seq-ordered-pairs
                                      (range (count bodies)))]
    (printf "%.9f\n" (energy planet-masses planet-positions planet-velocities))
    (loop [i (int n)
           planet-velocities planet-velocities
           planet-positions planet-positions]
      (if (zero? i)
        (printf "%.9f\n" (energy planet-masses planet-positions
                                 planet-velocities))
        (let [[new-velocities new-positions]
              (advance planet-masses planet-positions planet-velocities
                       delta-t)]
          (recur (unchecked-dec i) new-velocities new-positions)))))
  (flush))
