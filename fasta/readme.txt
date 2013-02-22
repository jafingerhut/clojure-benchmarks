As of Sep 15, 2012, the fastest Clojure programs for the fasta problem
were the following.  This is the same fastest to slowest order for all
4 benchmark machines, so I won't bother with the detailed timings for
each one.

fasta.clojure-5.clojure is the same as fasta.clojure-3.clojure, except
it has some macros added so that it will compile and run on Clojure
1.2, 1.3, and 1.4.

fasta.clojure-2.clojure
fasta.clojure

fasta.clojure-3.clojure failed to compile, because web site is using
Clojure 1.4 now, but this program has some function calls specific to
Clojure 1.2

----------------------------------------------------------------------

The "lineage" of these programs appears to be in numerical order, that
is, in the following list, each was a modification derived from the
previous one in the list.

fasta.clojure (identical to my fasta.clj-5.clj, except it has (. System (exit 0)) call at end)
fasta.clojure-2.clojure (identical to my fasta.clj-6.clj, except it has (. System (exit 0)) call at end)
fasta.clojure-3.clojure (almost same as fasta.clj-8.clj, except only compiles with Clojure 1.2)
fasta.clojure-5.clojure (identical to my fasta.clj-8.clj)

My recommendation is to keep at least the last one on the Benchmarks
Game web site, and if it is desired to keep the fastest 2, also keep
fasta.clojure-2.clojure.

Remove these two:

fasta.clojure
fasta.clojure-3.clojure
