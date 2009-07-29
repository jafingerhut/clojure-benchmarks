
(comment

(defn line-seq-of-file
  "Return sequence of strings that are the lines of the input file fname.  Not lazy -- reads whole file every time."
  [fname]
  (with-open [fr (java.io.FileReader. fname)
              br (java.io.BufferedReader. fr)]
    ;; Use doall to force reading of entire input file
    (doall (line-seq br))))


(defn fasta-from-lines
  "Return a sequence of maps {:desc s1 :dna-seq s2}, given as input a sequence of input lines from a FASTA format file.  Does not handle comment lines.  See http://en.wikipedia.org/wiki/Fasta_format"
  [lines]
  (lazy-seq
    (when-let [ls (seq lines)]
      ;; TBD: Verify that description-str begins with a \> character
      (let [description-str (subs (first ls) 1)  ; Chop off leading \>
            [lines-of-1st-dna-str remaining-lines]
               (split-with (complement fasta-description-line) (rest ls))
            dna-str (delay (.toUpperCase (apply str lines-of-1st-dna-str)))]
        (cons {:desc description-str :dna-seq dna-str}
              (fasta-from-lines remaining-lines))))))

  (def ls (line-seq-of-file "/Users/Shared/doc/language-shootout/fasta/sbcl-output.txt"))
  (count ls)
  (def fs (fasta-from-lines ls))
  (count fs)
  (map (fn [x] [(:desc x) (count (:dna-seq x))]) fs)
)


(comment
;; Reading in entire file with line-seq-of-file takes too much memory
;; for full-sized input file.

(defn dna-seq-of-rec-with-desc
  [fasta-rec desc-str]
  (when (= desc-str (subs (:desc fasta-rec) 0 (count desc-str)))
    (:dna-seq fasta-rec)))

(let [input-fname "knucleotide-input.txt"
      output-fname "clj-output.txt"
      fasta-recs (fasta-from-lines (line-seq-of-file input-fname))
      dna-str (force (some #(dna-seq-of-rec-with-desc % "THREE") fasta-recs))]
  (binding [*out* (java.io.FileWriter. output-fname)]
    (println (format "%d %s" (count dna-str) dna-str))
    ))
)


(commment

;; Still too much memory.  I guess I'll bite the bullet and make a fn
;; that is given the desired description string "THREE" before it
;; starts scanning the input lines, and only gets the desired one.

(let [input-fname "/Users/Shared/doc/language-shootout/fasta/sbcl-output.txt"
      output-fname "clj-output.txt"]
  (with-open [fr (java.io.FileReader. input-fname)
              br (java.io.BufferedReader. fr)]
    (binding [*out* (java.io.FileWriter. output-fname)]
      (let [lines (line-seq br)
            fasta-recs (fasta-from-lines lines)
            dna-str (force (some #(dna-seq-of-rec-with-desc % "THREE") fasta-recs))]

;;      (dorun (map #(println (format "%d %s" %1 %2))
;;                  (iterate inc 1) lines))
        (println (format "dna-str has len %d" (count dna-str)))
        (println dna-str)

        )
      ) ; (binding [*out ...]
    ))

)
