These are the four Clojure programs for the spectralnorm problem as of
Sep 15 2012:

spectralnorm.clojure-6.clojure
spectralnorm.clojure-7.clojure
spectralnorm.clojure-5.clojure
spectralnorm.clojure-2.clojure

They appear in that order from fastest to slowest on all four
benchmark machines, except for the 64-bit quad core machine where -7
is slightly faster than -6.

In all cases, -5 and -2 are significantly slower than -6 and -7.

Recommendation: Keep -6 and -7.  Remove -5 and -2.
