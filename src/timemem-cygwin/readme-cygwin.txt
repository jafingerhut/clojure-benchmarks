Sun Oct 31 14:52:55 PDT 2010
Andy Fingerhut
andy_fingerhut@alum.wustl.edu

timemem-cygwin.c is an earlier attempt of mine to make a program like
timemem-mingw.c in the clojure-benchmarks/src/timemem-mingw directory.
It was via this program and later learning of the Cygwin /usr/bin/time
precompiled package that I came to realize that if you attempt to
measure the performance of a Win32 binary from within Cygwin, you are
most likely to instead measure the performance of a hidden Cygwin bash
process instead, and thus quietly get misleading results (i.e. by
"quietly" I mean "without any warnings I could find in the command
output or the Cygwin documentation, although it might exist somewhere
and I didn't find it).

If you want to know more of the gory details, see these messages:

    http://cygwin.ru/ml/cygwin/2001-09/msg00202.html
    http://cygwin.ru/ml/cygwin/2001-09/msg00205.html

clojure-benchmarks/src/timemem-mingw/timemem-mingw.c is the
recommended over any program in this directory, which is only included
here in case later I learn of a way to measure what I want using it or
something similar to it.
