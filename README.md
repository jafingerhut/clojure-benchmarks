# Introduction

I worked on this pretty extensively around 2011-2013, submitting
faster Clojure programs for the problems on the [Computer Language
Benchmarks
Game](https://benchmarksgame-team.pages.debian.net/benchmarksgame/index.html)
web site.

Several other people, most notably Alex Miller, then spent a chunk of
time improving on most of my solutions, and submitting those to that
site as well.

Then some time around April 2017 the maintainer of that site decided
not to include the Clojure programs and performance measurements any
longer.

You can see Clojure programs from an archived copy of the site from
2017-Mar-31
[here](https://web.archive.org/web/20170331153459/http://benchmarksgame.alioth.debian.org/).
They are not present in the next dated snapshot of the site fro
2017-Apr-29
[here](https://web.archive.org/web/20170429193853/http://benchmarksgame.alioth.debian.org/)

[Here](https://tratt.net/laurie/blog/entries/why_arent_more_users_more_happy_with_our_vms_part_1.html)
is an article by Laurence Tratt that describes efforts he and a team
of researchers went to in order to carefully control run time
variances on the machine where they ran these and other benchmark
programs, and describes some odd performance behavior they found for
just-in-time compiling virtual machines for several programming
languages, including cases where the performance never seemed to
stabilize, or got worse after a period of time, and stayed worse.

None of the performance data I have collected went to anywhere near
that kind of effort to control the variations of results.  My best
effort was not to run any other significant programs intentionally on
the computer where the measurements were performed, and run each
benchmark 3 times, many hours apart from each other, with different
JVM processes started for each, in hopes of avoiding issues like cron
jobs taking significant CPU on one of those runs.


# Usage

Quick start instructions (see below for a few more details of what
these do if you are curious):

    (1) Check below for install instructions specific to your OS.

    Either: (2a) Install Leiningen (see next paragraph), or (2b) edit
    env.sh to specify the location of your Clojure JAR files.  Look
    for the CLOJURE_CLASSPATH variable settings.

    (2a) Leiningen version 2.x installation instructions:
    http://github.com/technomancy/leiningen

    Save Leiningen 2.x as 'lein' somewhere in your command path.
    Then use the following script to install multiple Clojure JAR
    files in your $HOME/lein directory.

    % ./lein-init.sh

    (3) Use init.sh to create largish input and expected output files
    (total about 850 Mbytes of storage), by running some Java
    programs:

    % ./init.sh output

    Time required on some of my systems: 6-7 mins, or 18 mins on a
    Windows + Cygwin running in a VM.

    (4) Run Clojure 1.3 versions of all of the benchmark programs:

    % ./run-all.sh clj-1.3

    Time required on some of my systems: 25 - 35 mins (about half the
    time was spent running the long fannkuch benchmark, and about 1/4
    on the long knucleotide benchmmark.)

    Another example: Run Java, Clojure 1.3, Clojure 1.4 alpha1, and
    Clojure 1.4 alpha3 versions of all of the benchmark programs.

    % ./run-all.sh long java clj-1.3 clj-1.4-alpha1 clj-1.4-alpha3

    (5) If you want all results recorded to an XML file, look in
    env.sh for MP_COMMON_ARGS and the comments before it on how to
    enable this.  Then do a ./run-all.sh command, or individual
    batch.sh commands as described below.  You can then convert the
    XML file to a CSV file for easier importing into spreadsheets
    with:

    % bin/xmltable2csv $HOME/results.xml > results.csv

Systems on which this has been tested:
* Mac OS X 10.5.8
* Mac OS X 10.6.8, both with and without MacPorts installed
* Mac OS X 10.7
* Ubuntu 10.4 LTS, 32-bit and 64-bit (also Ubuntu 11.10 and 12.04)
  Windows XP SP3/Vista/7 + Cygwin

You need a Java Development Kit installed.  A Java Virtual Machine
with no Java compiler (javac) is not enough.  You need a compiler for
Java source files that are used in the "./init.sh output" step above
to create the input files.  You also need a JVM to run Leiningen, if
you use that.

Tested with recent Java 1.6.0.X and 1.7.0.X HotSpot JVMs from Sun, and
JRockit from Oracle on Windows XP and Vista.


## Install instruction specific to Mac OS X

The executable program bin/timemem-darwin must be run as root on Mac
OS X 10.6 (Snow Leopard).  This is not needed for 10.5 (Leopard).  See
below for more details if you are curious.  One way to do this is to
make the program setuid root by running the commands below, entering
your password when prompted:

```bash
% sudo chown root bin/timemem-darwin
% sudo chmod 4555 bin/timemem-darwin
```

The following MacPorts packages can be useful.  There may be
equivalents under Homebrew -- I haven't tried them.

* gmp - If you want to run those pidigits programs that use the GNU MP
  library.
* git-core - optional, but useful for getting updated versions of
  clojure-contrib from github.com
* gcc44 - Big, takes a while to install, and only useful if you want to
  compile some of the parallel C memory bandwidth benchmarking
  programs in src/stream.
* p5-xml-libxml - Required if you have installed some version of perl
  within MacPorts (or more likely, it was installed as a dependency
  required by something else you did choose to install).  You can
  tell if the output of 'which perl' shows /opt/local/bin/perl or
  similar.  Without this package, using a MacPorts-installed Perl
  will likely result in error messages like the one below when you
  try to run programs with measureproc (invoked by batch.sh):

```
Can't locate XML/LibXML.pm in @INC (@INC contains: /opt/local/lib/perl5/site_perl/5.12.3/darwin-multi-2level /opt/local/lib/perl5/site_perl/5.12.3 /opt/local/lib/perl5/vendor_perl/5.12.3/darwin-multi-2level /opt/local/lib/perl5/vendor_perl/5.12.3 /opt/local/lib/perl5/5.12.3/darwin-multi-2level /opt/local/lib/perl5/5.12.3 /opt/local/lib/perl5/site_perl /opt/local/lib/perl5/vendor_perl .) at ../bin/measureproc line 22.
BEGIN failed--compilation aborted at ../bin/measureproc line 22.
```

You should have a Sun/Oracle/Apple Hotspot JDK installed by default
already with java* commands in /usr/bin and some other support files
(headers for JNI, etc.) in subdirectories beneath
/System/Library/Frameworks/JavaVM.framework

More details on bin/timemem-darwin root permissions:

It needs this permission in order to measure the memory usage of the
benchmark programs, for the same reasons that top(1) and ps(1) need to
run as root.  You can examine the source code of the program in
src/timemem-darwin/timemem-darwin.c if you want to know what it is
doing.  The only thing it needs root permission for is using Mach OS
calls to get the current memory usage of a child process.  It seems
getrusage() and wait4() system calls no longer measure the maximum
resident set size of a child process in OS X 10.6, like they used to
in 10.5.  I've filed a bug on Apple's developer web site in January
2011, but this behavior change may be intentional for all I know.


## Install instructions specific to Ubuntu Linux

On Ubuntu 11.10 (and perhaps others), the package libxml-libxml-perl
must be installed.  See below.

If you want the Sun/Oracle JDK, not OpenJDK (which is the default on
Ubuntu as of 10.4, or perhaps even earlier versions of Ubuntu), follow
these steps:

```bash
% sudo add-apt-repository "deb http://archive.canonical.com/ lucid partner"
```

After that, you can either run the following commands, or use the
graphical Synaptic Package Manager to add the sun-java6-jdk package.

```bash
% sudo apt-get update
% sudo apt-get install sun-java6-jdk
```

The following packages are also useful to add on Ubuntu Linux:

* libxml-libxml-perl - Required for measureproc to work.
* libgmp3-dev - If you want to run those pidigits programs that use the
    GNU MP library.
* gcc - To compile the C pidigits program.
* git-core - optional, but useful for getting updated versions of
    clojure-contrib from github.com

Source:
http://happy-coding.com/install-sun-java6-jdk-on-ubuntu-10-04-lucid/


## Install instructions specific to Windows+Cygwin

Cygwin can be obtained from http://www.cygwin.com

Besides the basic packages added with every Cygwin installation, you
must install at least some of the following for this software to work:

* perl (category Interpeters and Perl) and libxml2 (many categories,
    including Devel) - needed if you want to run any of the included
    Perl scripts, which includes bin/measureproc, the recommended way
    to make time and memory measurements of the benchmark programs.
* wget (in category Web) - needed if you use Leiningen
* git (category Devel) - optional, but useful for getting updated
    versions of clojure-contrib from github.com
* gcc - If you want to run the C version of the pidigits program.
* libgmp-devel - If you want to run those pidigits programs that use the
    GNU MP library.

Edit env.sh so that JAVA_BIN points at the directory where your java
and javac commands are installed.  Examples are there already for the
default install locations of JRockit and Hotspot JVMs (at least
particular version numbers of them).  If you install the Sun/Oracle
Hotspot JDK, its default location is C:\Program
Files\Java\jdk<version>.  For Oracle JRockit the default install
location is C:\Program Files\Java\jrmc-<version>.

I've tried to make all bash scripts handle spaces in filenames by
using "${BASH_VARIABLE_NAME}" syntax to substitute the values of shell
variables.  There is some weirdness in some places in the scripts
because Cygwin commands accept paths in Unix notation with /
separators, but when invoking the JVM commands, they often require
Windows path names with \ separators.  The Cygwin command 'cygpath' is
useful in converting between these two formats.

Caveat: I don't yet know how to compile the JNI interface to the GMP
library so that the Java, Scala, or Clojure programs using that native
library can run.  The gcc4-core (category Devel) and libgmp-devel
(category Libs and Math) Cygwin packages can certainly help in
compiling the files, but so far I haven't compiled them in a way that
they can be used by a JVM running on Windows.  Please let me know if
you figure out how to do this.

The Perl, SBCL, and Haskell programs have been well tested on Mac OS
X, and more lightly tested on Linux and Windows.  The main issue is
getting the relatively recent versions of SBCL and GHC, and some
benchmark programs require additional libraries on top of that
(e.g. for regex matching).


The non-Clojure versions of these benchmark programs have been
downloaded from this web site:

http://benchmarksgame.alioth.debian.org

See the file COPYING for the licenses under which these files are
distributed.

So far I have Clojure implementations for the benchmarks noted in the
table below.

```
                                           Clojure
                    Clojure    Easy to	   program is
Benchmark           program    parallel-   parallel-
                    written?   ize?        ized?
------------------- ---------- ----------- ----------
binarytrees           yes        yes        yes
chameneos-redux        -         yes         -
------------------- ---------- ----------- ----------
fannkuch              yes        yes        yes
   (^^^ deprecated on benchmarks web site)
fannkuchredux         yes        yes        yes
fasta                 yes         no         no
------------------- ---------- ----------- ----------
knucleotide           yes        yes        yes
mandelbrot            yes        yes        yes
meteor-contest         -          ?          -
------------------- ---------- ----------- ----------
nbody                 yes         no         no
pidigits              yes         no         no
regexdna              yes      part of it   yes
------------------- ---------- ----------- ----------
revcomp               yes         no         no
spectralnorm          yes        yes        yes
thread-ring            -         yes         -
------------------- ---------- ----------- ----------
```


I have hacked up some shell scripts to automate the compilation and
running of some of these programs.  They have been tested on Mac OS X
10.5.8 and 10.6.8 with recent versions of the Glasgow Haskell Compiler
ghc, SBCL Common Lisp, Perl, several versions of Sun's Java VM (for
Windows XP, Linux, and Mac), Oracle's JRockit JVM for Windows, and
Clojure 1.2.0 and 1.3.0 alpha1 (and earlier 1.0.0 or shortly before
that release).  Some of the benchmarks use Clojure transients and thus
require 1.0 or later, and some use deftype, requiring Clojure 1.2.0 or
later (or whenever deftype was introduced).  See below for the steps I
used to install SBCL and ghc.

You should edit the file env.sh to point at the location of your
Clojure JAR files, and perhaps give explicit path names to some of the
other language implementations, if you wish.

If you want to create Clojure JAR files in the locations already
specified in the file env.sh, you can install Leiningen and then run
the following script.  Doing so will create directories named 'lein'
and '.m2' in your home directory, if they do not already exist, and
fill them with a few dozen files each.

Note: This step is optional, but if you do not do it, you must either
edit env.sh to point at your Clojure JAR files, or put Clojure JAR
files in the locations mentioned in that file.

```bash
% ./lein-init.sh
```

The knucleotide, regexdna, and revcomp benchmark input files are quite
large (250 Mbytes), and not included in the distribution.  These input
files must be generated by running this shell script:

```bash
% ./init.sh
```

That will generate the input files only.  You can also choose to
generate those plus the "expected output files" (which are just the
output of the Java versions of the benchmark programs) by running
this:

```bash
% ./init.sh output
```

If you have the input files generated, you can run all of the
implementations of the knucleotide benchmark, with the quick, medium,
and long test input sizes, using these commands:

```bash
% cd knucleotide
% ./batch.sh
```

You can also pick and choose which benchmark lengths to run, and which
language implementations to run, by giving options like these on the
command line:

```bash
% ./batch.sh java clj-1.2 clj-1.3-alpha1 quick medium
```

That would run the Java, Clojure 1.2, and Clojure 1.3 alpha1 versions
of the benchmark, each with the quick and medium size input files.
Note that the order of the command line arguments is not important.
The following command would run only the Clojure 1.2 version with the
long input file:

```bash
% ./batch.sh clj-1.2 long
```

You can also run the following command from the root directory of this
package, and it will run a batch.sh command with the same command line
arguments in each of the benchmark subdirectories, e.g.

```bash
./run-all.sh long java clj-1.2
```

will run the long benchmark for Java and Clojure 1.2 in all of the
subdirectories mentioned in the 'for' line you can see for yourself in
the run-all.sh script.

Note that all of the benchmarks game web site results are for what are
called the 'long' benchmarks in this package.  The short and medium
tests are primarily for quicker cycling through the
edit-compile-run-debug loop, when you are developing a benchmark
program yourself.

If you find any improvements to the Clojure versions, I'd love to hear
about them.

The files RESULTS-clj-1.1 and RESULTS-clj-1.2 in the results directory
contains some summarized execution times from running these programs
on my home iMac.  The file results-java-clj-1.2-1.3a1.xls is an Excel
spreadsheet containing run time results for several different JVMs and
operating systems, all on the same hardware as each other (but not the
same hardware as the RESULTS-clj-1.1 and RESULTS-clj-1.2 files above,
so don't go comparing results between these files to each other
directly).

Andy Fingerhut
andy_fingerhut@alum.wustl.edu


----------------------------------------------------------------------
On a Mac OS X machine (tested on 10.5.8 and 10.6.4 at least), download
and install MacPorts from here:

http://www.macports.org

After following the instructions there for installing MacPorts, you
can install the Glasgow Haskell compiler with the command:

```bash
% sudo port install ghc
```

And SBCL with the threads (i.e. multi-threading) option enabled with
this command:

```bash
% sudo port install sbcl@+threads
```


Here are the versions I currently have installed used to produce some
of my benchmark results:

```bash
% port installed ghc sbcl
The following ports are currently installed:
  ghc @6.10.1_8+darwin_9_i386 (active)
  sbcl @1.0.24_0+darwin_9_i386+html+test+threads (active)

% java -version
java version "1.6.0_13"
Java(TM) SE Runtime Environment (build 1.6.0_13-b03-211)
Java HotSpot(TM) 64-Bit Server VM (build 11.3-b02-83, mixed mode)

% javac -version
javac 1.6.0_13

% sbcl --version
SBCL 1.0.29

% ghc --version
The Glorious Glasgow Haskell Compilation System, version 6.10.1
```

----------------------------------------------------------------------

I've also done some testing of this set of scripts on an Ubuntu 10.04
Desktop i386 installation, in a VMWare Fusion virtual machine running
on my Mac (and earlier tested on Ubuntu 9.04).

I've tried it with these versions of packages installed using Ubuntu's
Synaptic Package Manager.

```
sun-java6-jdk 6-14-0ubuntu1.9.04
sbcl 1.0.42 (and earlier tested with 1.0.18.0-2)
ghc 6.8.2dfsg1-1ubuntu1
```


```bash
% java -version
java version "1.6.0_14"
Java(TM) SE Runtime Environment (build 1.6.0_14-b08)
Java HotSpot(TM) Client VM (build 14.0-b16, mixed mode, sharing)

% javac -version
javac 1.6.0_14

% sbcl --version
SBCL 1.0.18.debian

% ghc --version
The Glorious Glasgow Haskell Compilation System, version 6.8.2
```

Apparently the Haskell knucleotide program requires GHC 6.10 or
later, and gives a compilation error with GHC 6.8.x, as is currently
the latest available through Ubuntu's distribution system.  If you
really want to run that benchmark, you could try installing GHC 6.10
yourself.  This web page may provide the right recipe for doing so.  I
haven't tried it myself.

http://www.johnmacfarlane.net/Gitit%20on%20Ubuntu


# Some features of Clojure that are definitely slow ...

... and whether these Clojure programs use those features

The following notes come from a reply to a question on Stack Overflow
about why the Clojure programs are slower than the Scala programs.

    http://stackoverflow.com/questions/4148382/why-does-clojure-do-worse-than-scala-in-the-alioth-benchmarks

Note that significant speed improvements were made to many of the
Clojure programs on the Computer Language Benchmarks Game web site
after this Stack Overflow discussion occurred, so what the discussion
participants saw then was noticeably slower (relative to Java -6
server) than what is on the web site now.

For example, the following features in Clojure are all very cool and
useful for development convenience, but incur some runtime performance
overhead:

* Lazy sequences and lists
* Dynamic Java interoperability using reflection
* Runtime function composition / first class functions
* Multimethods / dynamic dispatch
* Dynamic compilation with eval or on the REPL
* BigInteger arithmetic

If you want absolute maximum performance (at the cost of some extra
complexity), you would want to rewrite code to avoid these and use
things like:

* Static type hinting (to avoid reflection)
* Transients
* Macros (for compile time code manipulation)
* Protocols
* Java primitives and arrays
* loop / recur for iteration

I have attempted to categorize which Clojure programs in this
collection use the features above, and which do not, in an Excel
spreadsheet.  See the file results/clojure-features-used.xls.  This
spreadsheet should be easily readable using Open Office or one of its
derivatives, as no "fancy features" are used in it.  It is simply a
more convenient way for me to record this information in table form,
and later rearrange and view them in different ways, than a text file.


# Do these programs fall into a microbenchmark pitfall?

Cliff Click's slides for his talk "How NOT To Write A Microbenchmark",
given at JavaOne 2002 (Sun's 2002 Worldwide Java Developer
Conference).

    http://www.azulsystems.com/events/javaone_2002/microbenchmarks.pdf

I mention this because (1) it is worth reading for anyone interested
in benchmarks, to avoid pitfalls, and (2) so I can make a list of
pitfalls mentioned in there, and for each Clojure program describe
whether I believe it has fallen into that pitfall.

Are the benchmarks game programs microbenchmarks?  Dr. Click gives his
definition on p. 8, and several examples throughout his talk.  The
benchmarks game programs are relatively small, and in some cases the
datasets are "large" (i.e. hundreds of megabytes, but none are a
gigabyte).  However, there is a significant difference between these
benchmarks vs. the examples in Dr. Click's slides: the goal with these
benchmarks is to measure the entire run time of the whole program, not
some smaller part of it.  We aren't trying to see whether a particular
function call takes 7 microsec versus 15 microsec.  We are trying to
find out how much time is required for all of the calls made together
during the entire run, and find a program that is as fast as possible
for that total time.

Now it is true that for most of these programs, 99% of the time is
spent in relatively small fraction of the lines of code.  For example,
in knucleotide.clj-8b.clj, almost all of the compute time is spent in
the last 13 lines of the function tally-dna-subs-with-len.  In
nbody.clj-12.clj, most of the time is spent in the last 10 lines of
the function advance, which calls the 5-line p-dt! and the 18-line
v-dt!, which in turn calls the 4-line v+!, for a total of 27 lines of
"hot code" in a 252-line file.

One piece of advice from Dr. Click's slides that is most relevant is
"Know what you test" (title of p. 32).  These benchmarks game programs
are not intended to discover some particular fact, and remove all
other variables, _unless_ your goal is to discover how well this
particular program performs on the task at hand.  For example, the
knucleotide benchmark programs do not measure only the performance of
the hash table implementation (because there is at least some work in
doing other things, like reading in a file that is hundreds of
megabytes long), but that is certainly the most significant portion of
the program's run time.

In general, when comparing Java and Clojure run times, or any two
languages implemented on the Java Virtual Machine in this collection
of benchmarks, realize that the following run times are _included_ in
the times that you see reported:

* JVM initialization time
* loading classes from compiled class files
* time spent reading the input data file (if any) and writing output
* any time spent doing JIT compilation
* any time spent performing garbage collection

The following times are _not_ included in the times you see reported:

* Compiling source files to class files

All of the Clojure programs are ahead of time (or AOT) compiled, and
compiled class files are saved on disk, before the measurement clock
is started.

Is this fair?  In the sense that the same kinds of processing time are
included in measurements of both Java and Clojure, it is fair.  Is it
what _you_ personally want to measure?  That depends upon what you
want to measure.  You are welcome to measure other things about these
programs if you wish, and report the times you get.

Because the goal here is to measure the entire run time of these
programs, "warming up the JVM" via JIT compilation is all included.
There is no reason (with this goal) to first run the JVM until it is
warmed up, and then run "the real test".  Warmup is part of the real
test.

In all of these programs, there should be little or no dead code to be
eliminated.  All results calculated are printed out in some form.
Sometimes only a small summary of the the calculated results are
actually printed, but it is always intended to be a summary such that
there are no known compilers that are "smart" enough to figure out
that they could reduce the processing required in order to print the
desird results.  Could a human do that?  In some cases, it is pretty
easy for a human to change the program to produce the same output in a
similar way.  For example, in the knucleotide benchmark the program
must print out the number of occurrences of the substring "GGT" in a
large input string.  However, the instructions for the benchmark
require that first a table is made that maps _all_ length 3 substrings
of the input string to a count of the number of occurrences, then
extract out the entry for "GGT" and print its count.  Obviously a
person can change the program to reduce the time and space required if
the goal is only to calculate counts for the substring "GGT", but it
is breaking the rules of the benchmark to do so.


Cliff Click's advice (summarized greatly):

* Is there loop unrolling going on behind the scenes? (pp. 17-18)

* "Beware 'hidden' Cache Blowout" - Does the data set fit into L1
   cache? (p. 28-29)

* Explicitly Handle GC: Either don't allocate in main loops, so no GC
  pauses, or run long enough to reach GC steady state. (p. 30) TBD:
  Consider adding instrumentation to measure total GC time in each run
  involving a JVM.

* Know what you test (p. 32) - use profiler to measure where most of
  the time is spent, and report that with the results.

* Be aware of system load - How to verify total CPU time taken by all
  other processes on the system?  I can eyeball it if I watch the
  Activity Monitor on a Mac, or top on Linux, Windows Task Manager on
  Windows, but is there an automated way to easily determine the total
  CPU time used by all other processes during a particular time
  interval?

* Be aware of clock granularity (p. 33): This should not be an issue
  since all of the Clojure programs run for at least 10 sec, and clock
  granularity is definitely better than 0.1 sec.  (TBD: Verify these
  numbers)

* Run to steady state, at least 10 sec (p. 33): In many of these
  programs, each iteration is doing something a bit different, but the
  general idea of running at least 10 sec is a good one.

* Fixed size datasets (p. 34): Every run with input data has the same
  input file.  For each one, state the size of the input data that
  actually needs to be "worked on".

* Constant amount of work per iteration (p. 34): This is not really
  relevant for these benchmarks, if we are only paying attention to
  the total time to complete the benchmark run.  It is only relevant
  if we are trying to calculate a time per iteration of some loop,
  which we are not.

* Avoid the 'eqntott Syndrome' (p. 34): Again, we are not looking at
  run times of separate iterations and comparing them to each other.
  There is some warmup time of doing JIT compilation in all of these
  runs, but it is there every single time, and it is there for Clojure
  _and_ Java, and any other JVM-based language implementation.

* Avoid 'dead' loops (p. 35): The final answer is always printed, and
  the computation is non-trivial in all cases I can recall.  Good to
  verify this for each benchmark program individually.

* Be explicit about GC (p. 36): Does this mean to measure it and
  report it separately?

* JIT performance may change over time.  Warmup loop + test code
  before ANY timing (p. 36): This is relevant if we want to measure
  the time per iteration of some loop.  That is not the goal for these
  benchmark programs.  We only care about the time to get the final
  result.

----------------------------------------------------------------------
Profiling results
----------------------------------------------------------------------

TBD: Record a summary for the fastest known Clojure programs, or maybe
all of them, where they spend the most time, i.e. the call stacks that
(combined) take up at least half of the program execution time,
according to a Java profiler.
