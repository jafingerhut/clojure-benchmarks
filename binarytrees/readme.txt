----------------------------------------------------------------------
As of Feb 21 2013 the fastest Clojure and Java programs on the 64-bit
4 core machine on the Computer Language Benchmarks Game web site were:

binarytrees.clojure = my binarytrees.clojure-rh.clojure except for white space and comments
binarytrees.java = my binarytrees.java-1.java except for white space and comments

----------------------------------------------------------------------
As of Sep 15 2012, here are the fastest Clojure programs for the
binarytrees problem, in order from fastest to slowest.  The order is
different on the 4 benchmark machines:

6 binarytrees.clojure-6.clojure
5 binarytrees.clojure-5.clojure identical to binarytrees.clj-4.clj
3 binarytrees.clojure-3.clojure same as clojure-4 except map instead of -4's pmap
1 binarytrees.clojure           identical to binarytrees.clj
4 binarytrees.clojure-4.clojure identical to binarytrees.clj-3.clj
2 binarytrees.clojure-2.clojure

    32-bit        64-bit
  1c     4c     1c     4c

   sec    sec    sec    sec
 6  39  6  29  6  24  6  18
 5  52  5  40  5  41  5  32

 3  69  4  47  3  50  3  40
 1  96  2  50  1  78  4  47
 4 111  3  54  4  98  2  52
 2 137  1  81  2 128  1  63

I recommend keeping -6 only, plus the binarytrees.clojure-rh.clojure
version that Rich Hickey created in March 2011, and at least on the
64-bit JVM I tested on was even faster than the -6 version.
