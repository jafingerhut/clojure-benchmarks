;;   The Computer Language Benchmarks Game
;;   http://benchmarksgame.alioth.debian.org/

;; contributed by Andy Fingerhut
;; modified by Marko Kocic
;; modified by Mike Anderson to make better use of primitive operations
;; modified by Andy Fingerhut for lower CPU use and high parallelism

;; Ideas for future enhancement: Better parallelism: Do completion of
;; DNA string reading while beginning hash table creation in parallel.
;; Lower memory use: Read DNA string as bytes instead of Java chars.

(ns knucleotide
  (:import java.util.concurrent.ExecutorService
           java.util.concurrent.Executors)
  (:gen-class))

(set! *warn-on-reflection* true)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This is a copy of part of Amit Rathore's Medusa package, which
;; allows you to submit a bunch of Clojure expressions to run to a
;; thread pool with a fixed size.  No more than that many threads will
;; ever run at once, but Medusa tries to keep that many threads going
;; at all times, as long as there are things to do that have been
;; submitted.  This is unlike Clojure's built-in pmap, which often
;; runs fewer threads in parallel if the run time of the jobs differs
;; significantly from each other.
;;
;; git clone http://github.com/amitrathore/clj-utils.git
;; git clone http://github.com/amitrathore/medusa.git
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def THREADPOOL)

(def running-futures (ref {}))

(defn create-runonce [function]
  (let [sentinel (Object.)
        result (atom sentinel)] 
    (fn [& args]
      (locking sentinel 
        (if (= @result sentinel)
          (reset! result (apply function args)) 
          @result)))))

(defmacro defrunonce [fn-name args & body]
  `(def ~fn-name (create-runonce (fn ~args ~@body))))

(defrunonce init-medusa [pool-size]
  (def THREADPOOL (Executors/newFixedThreadPool pool-size)))

(defn claim-thread [future-id]
  (let [thread-info {:thread (Thread/currentThread) :future-id future-id
                     :started (System/currentTimeMillis)}]
    (dosync (alter running-futures assoc future-id thread-info))))

(defn mark-completion [future-id]
  (dosync (alter running-futures dissoc future-id)))

(defn medusa-future-thunk [future-id thunk]
  (let [^Callable work (fn []
                         (claim-thread future-id)
                         (let [val (thunk)]
                           (mark-completion future-id)
                           val))]
    (.submit ^ExecutorService THREADPOOL work)))

(defn random-uuid []
  (str (java.util.UUID/randomUUID)))

(defmacro medusa-future [& body]
  `(medusa-future-thunk (random-uuid) (fn [] (do ~@body))))

(defn medusa-pmap [num-threads f coll]
  (if (== num-threads 1)
    (map f coll)
    (do
      (init-medusa num-threads)
      (let [seq-of-futures (doall (map #(medusa-future (f %)) coll))]
        (map (fn [java-future] (.get ^java.util.concurrent.Future java-future))
             seq-of-futures)))))

(defn shutdown-medusa []
  (.shutdown ^ExecutorService THREADPOOL))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This is the end of the subset of Medusa code.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmacro key-type [num]
  `(long ~num))

(definterface IFragment
  (set_key_BANG_ [^long k])
  (^long get_key [])
  (inc_BANG_ [])
  (add_BANG_ [^int n])
  (^int get_count []))


(deftype Fragment [^{:unsynchronized-mutable true :tag long} key
                       ^{:unsynchronized-mutable true :tag int} cnt]
  Object
  ;; TBD: Is there a way to return an int hashCode that is a truncated
  ;; version of the long value key without using bit-and?  Simply
  ;; using (int key) throws an exception if key is larger than
  ;; Integer/MAX_VALUE, e.g. (int Long/MAX_VALUE).
  (^int hashCode [this]
    (int (bit-and key Integer/MAX_VALUE)))
  (^boolean equals [this ^Object o]
    (let [^Fragment f o]
      (== key (.key f))))

  IFragment
  (set-key! [this ^long k]
    (set! key k))
  (get-key [this] key)
  (inc! [this]
    (set! cnt (unchecked-inc-int cnt)))
  (add! [this ^int n]
    (set! cnt (unchecked-add-int cnt n)))
  (get-count [this] cnt))

 

;; Return true when the line l is a FASTA description line

(defn fasta-description-line [l]
  (= \> (first (seq l))))


;; Return true when the line l is a FASTA description line that begins
;; with the string desc-str.

(defn fasta-description-line-beginning [desc-str l]
  (and (fasta-description-line l)
       (= desc-str (subs l 1 (min (count l) (inc (count desc-str)))))))


;; Take a sequence of lines from a FASTA format file, and a string
;; desc-str.  Look for a FASTA record with a description that begins
;; with desc-str, and if one is found, return its DNA sequence as a
;; single (potentially quite long) string.  If input file is big,
;; you'll save lots of memory if you call this function in a with-open
;; for the file, and don't hold on to the head of the lines parameter.

(defn fasta-dna-str-with-desc-beginning [desc-str lines]
  (when-let [x (drop-while
		(fn [l] (not (fasta-description-line-beginning desc-str l)))
		lines)]
    (when-let [x (seq x)]
      (let [y (take-while (fn [l] (not (fasta-description-line l)))
                          (map (fn [#^java.lang.String s] (.toUpperCase s))
                               (rest x)))]
        (apply str y)))))


(def dna-char-to-code-val-map {\A 0, \C 1, \T 2, \G 3})
(def code-val-to-dna-char {0 \A, 1 \C, 2 \T, 3 \G})

(defmacro dna-char-to-code-val [ch]
  `(case ~ch
     ~@(flatten (seq dna-char-to-code-val-map))))

;; In the hash map 'tally' in tally-dna-subs-with-len, it is more
;; straightforward to use a Clojure string (same as a Java string) as
;; the key, but such a key is significantly bigger than it needs to
;; be, increasing memory and time required to hash the value.  By
;; converting a string of A, C, T, and G characters down to an integer
;; that contains only 2 bits for each character, we make a value that
;; is significantly smaller and faster to use as a key in the map.

;;    most                 least
;; significant          significant
;; bits of int          bits of int
;;  |                         |
;;  V                         V
;; code code code ....  code code
;;  ^                         ^
;;  |                         |
;; code for               code for
;; *latest*               *earliest*
;; char in                char in
;; sequence               sequence

;; Note: Given Clojure 1.2's implementation of bit-shift-left/right
;; operations, when the value being shifted is larger than a 32-bit
;; int, they are faster when the shift amount is a compile time
;; constant.

(defn ^:static dna-str-to-key 
  (^long [^String s] (dna-str-to-key s 0 (count s)))
  (^long [^String s ^long start ^long length]
    ;; Accessing a local let binding is much faster than accessing a var
    (loop [key (long 0)
           offset (int (+ start length -1))]
      (if (< offset start)
        key
        (let [c (.charAt s offset)
              code (int (dna-char-to-code-val c))
              new-key (+ (bit-shift-left key 2) code)]
          (recur new-key (dec offset)))))))


(defn key-to-dna-str [^Fragment f len]
  (let [k (.get-key f)]
    (apply str (map code-val-to-dna-char
                    (map (fn [pos] (bit-and 3 (bit-shift-right k pos)))
                         (range 0 (* 2 len) 2))))))

;; required function : "to update a hashtable of k-nucleotide keys and
;; count values, for a particular reading-frame"

(defn tally-dna-subs-with-len [len dna-str start-offset end-offset]
  (let [len (int len)
        start-offset (int start-offset)
        dna-str ^String dna-str
        mask-width (* 2 len)
        mask (key-type (dec (bit-shift-left 1 mask-width)))]
    (loop [offset (int end-offset)
           key (key-type (dna-str-to-key dna-str offset len))
           tally (let [h (java.util.HashMap.)
                       one (Fragment. (long key) (int 1))]
                   (.put h one one)
                   h)
           fragment (Fragment. (long 0) (int 1))]
      (if (<= offset start-offset)
        tally
        (let [new-offset (unchecked-dec offset)
              new-first-char-code (dna-char-to-code-val
                                   (.charAt dna-str new-offset))
              new-key (key-type (bit-and mask (unchecked-add (bit-shift-left key 2)
                                                             new-first-char-code)))]
          (.set-key! fragment new-key)
          (if-let [^Fragment cur-fragment (get tally fragment)]
            (do
              (.inc! cur-fragment)
              (recur new-offset new-key tally fragment))
            (do
              (let [new-fragment (Fragment. (long 0) (int 1))]
                (.put tally fragment fragment)
                (recur new-offset new-key tally new-fragment)))))))))


(defn ^:static getcnt ^long [^Fragment tc]
  (.get-count tc))

(defn ^:static tally-total [tally]
  (loop [acc (long 0)
         s (vals tally)]
    (if-let [v (first s)]
      (recur (+ acc (getcnt v)) (next s))
      acc)))

(defn all-tally-to-str [tally fn-key-to-str]
  (with-out-str
    (let [total (tally-total tally)
          cmp-keys (fn [k1 k2]
                     ;; Return negative integer if k1 should come earlier
                     ;; in the sort order than k2, 0 if they are equal,
                     ;; otherwise a positive integer.
                     (let [cnt1 (int (getcnt (get tally k1)))
                           cnt2 (int (getcnt (get tally k2)))]
                       (if (not= cnt1 cnt2)
                         (- cnt2 cnt1)
                         (let [^String s1 (fn-key-to-str k1)
                               ^String s2 (fn-key-to-str k2)]
                           (.compareTo s1 s2)))))]
      (doseq [k (sort cmp-keys (keys tally))]
        (printf "%s %.3f\n" (fn-key-to-str k)
                (double (* 100 (/ (getcnt (get tally k)) total))))))))


(defn one-tally-to-str [dna-str tallies]
  (let [zerotc (Fragment. 0 0)
        ^Fragment f (Fragment. (long (dna-str-to-key dna-str)) 0)]
    (format "%d\t%s" (reduce + (map #(getcnt (get % f zerotc))
                                    tallies))
            dna-str)))


(defn piece-sizes [total-units n-pieces]
  (let [min-units-per-piece (quot total-units n-pieces)
        n-pieces-with-1-extra (mod total-units n-pieces)]
    (take n-pieces (concat (repeat n-pieces-with-1-extra
                                   (inc min-units-per-piece))
                           (repeat min-units-per-piece)))))


(defn break-work-into-pieces [{:keys [kind n-pieces] :as m} dna-str]
  (let [substr-len (if (= kind :tally-all) (:substr-len m) (count (:substr m)))
        n-substrs (inc (- (count dna-str) substr-len))
        sizes (piece-sizes n-substrs n-pieces)
        start-end-offsets (map (fn [[a b]] [a (dec b)])
                               (partition 2 1 (cons 0 (reductions + sizes))))]
    (assert (= n-substrs (reduce + sizes)))
    (for [[start end+1] (partition 2 1 (cons 0 (reductions + sizes)))]
      (assoc m :substr-len substr-len
             :dna-str dna-str
             :start-offset start
             :end-offset (dec end+1)))))


(defn do-one-piece [{:keys [substr-len dna-str start-offset end-offset] :as m}]
  (assoc m :tally-table (tally-dna-subs-with-len substr-len dna-str
                          start-offset end-offset)))

;; Like merge-with, except it only works for the HashMaps with
;; Fragments as key/value pairs.  It mutates the first HashMap given
;; in place, and potentially some of the Fragments in all of the
;; hashmaps.
(defn add-tally-hashmaps! [hmaps]
  (let [merge-entry (fn [^java.util.HashMap hm e]
                      (let [k (key e) v (val e)]
                        (if (contains? hm k)
                          (let [^Fragment cur-fragment (get hm k)
                                n (int (getcnt v))]
                            (.add! cur-fragment n))
                          (.put hm k v)))
                      hm)
        merge2 (fn [hm1 hm2]
                 (reduce merge-entry hm1 (seq hm2)))]
    (reduce merge2 hmaps)))


;; Combine pieces with same :substr-len into one final result
;;
;; For :tally-all, this should combine multiple tally tables into one
;; combined table, then print out the contents of the table.
;;
;; For :tally-one, this should extract out the counts for the one
;; desired string from each table, sum them, and print that result.
;; TBD: Is it within the rules to do that, or must it produce as an
;; intermediate result one hash table that is the sum of all of the
;; partial hash tables?
(defn combine-pieces [pieces]
  (let [p (first pieces)
        kind (:kind p)
        substr-len (:substr-len p)]
    (case kind
      :tally-all {:substr-len substr-len
                  :output (all-tally-to-str
                           (add-tally-hashmaps! (map :tally-table pieces))
                           (fn [k] (key-to-dna-str k substr-len)))}
      :tally-one {:substr-len substr-len
                  :output (one-tally-to-str (:substr p)
                                            (map :tally-table pieces))})))


(defn run [br]  
  (let [n-threads (.. Runtime getRuntime availableProcessors)
        dna-str (fasta-dna-str-with-desc-beginning "THREE" (line-seq br))
        work-pieces-todo
        (mapcat #(break-work-into-pieces % dna-str)
                [{:kind :tally-all :n-pieces 1 :substr-len 1}
                 {:kind :tally-all :n-pieces 1 :substr-len 2}
                 {:kind :tally-one :n-pieces 2 :substr "GGT"}
                 {:kind :tally-one :n-pieces 2 :substr "GGTA"}
                 {:kind :tally-one :n-pieces 3 :substr "GGTATT"}
                 {:kind :tally-one :n-pieces 3 :substr "GGTATTTTAATT"}
                 {:kind :tally-one :n-pieces 4 :substr "GGTATTTTAATTTATAGT"}])
        work-pieces-done (medusa-pmap n-threads do-one-piece work-pieces-todo)
        grouped-results (partition-by :substr-len work-pieces-done)
        combined-results (pmap combine-pieces grouped-results)
        results (sort-by :substr-len combined-results)]
    (doseq [r results]
      (println (:output r))
      (flush))))

(defn -main [& args]
  (with-open [br (java.io.BufferedReader. *in*)]
    (run br))  
  (shutdown-agents)
  (shutdown-medusa))
