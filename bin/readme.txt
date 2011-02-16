Sun Oct 31 15:01:23 PDT 2010
Andy Fingerhut
andy_fingerhut@alum.wustl.edu

The following files are compiled from sources elsewhere in this
repository.

----------------------------------------------------------------------

JVMInfo.class - src/jvm-info/JVMInfo.java

Compiled using javac on a PowerPC-based Mac, because its older version
of the JDK could produce .class files that run without warning on that
machine, but when compiled on newer JDKs the older one gave a warning.

----------------------------------------------------------------------

showenv.exe - src/timemem-mingw/showenv.c
timemem.exe - src/timemem-mingw/timemem-mingw.c

Both of the above were compiled using the Makefile in that directory
in a MinGW shell window on Windows (http://www.mingw.org).  See the
file readme-timemem.txt for more documentation on timemem.exe

----------------------------------------------------------------------

timemem-darwin - src/timemem-darwin/timemem-darwin.c

Compiled on an Intel Mac, but it is compiled to run on both Intel and
PowerPC architectures.  See the Makefile in that directory.

----------------------------------------------------------------------
