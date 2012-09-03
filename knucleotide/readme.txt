As of Aug 26, 2012, the fastest Clojure programs for the knucleotide
program were as detailed below.

I believe they are in order from oldest to newest, at least as of when
they were submitted to the Alioth Benchmarks Game web site.  They are
also roughly in order from slowest to fastest.

knucleotide.clojure is nearly identical to my knucleotide.clj-9.clj,
except white space and comment differences, one has printf
vs. (println (format ...)), and the first has an explicit call to exit
that improves performance measurements on parallel benchmarks because
of the background threads hanging around doing nothing for 1 min.

knucleotide.clojure-2.clojure is nearly identical to my
knucleotide.clj-8.clj.  The differences are as minor as in the
previous case.

knucleotide.clojure-3.clojure = knucleotide.clj-14-web-site.clj, and
nearly identical to knucleotide.clojure-4.clojure.  #3 fails to
compile with Clojure 1.4 (and maybe 1.3).  #4 has only changes needed
for #3 to compile with Clojure 1.4, so should definitely be removed,
whether #4 is or not.

knucleotide.clojure-4.clojure = knucleotide.clj-14-for-clj13.clj

knucleotide.clojure-5.clojure is nearly identical to my
knucleotide.clj-14-for-clj13.clj

knucleotide.clojure-6.clojure seems to start with 5, and make
several performance optimizations from there.  It is nearly identical
except for whitespace to knucleotide.clj-15.clj

knucleotide.clojure-6c-minoredits.clojure is nearly identical to
knucleotide.clojure-6.clojure.  It removes some vestigial code that
allowed it to compile and run with Clojure 1.2, and some code that
allows it to read from a resource file instead of standard input that
isn't needed.  It also adds a start-offset and end-offset parameter to
tally-dna-subs-with-len that is a step towards making calls to this
function for the same substring length, each operating in parallel on
different portions of dna-str.  Its performance is identical to
knucleotide.clojure-6.clojure.

knucleotide.clojure-6d-java9inspired.clojure
Use a class like the Fragment class in knucleotide.java-9.java.  This
is used as both a key and value in the JavaHashMap's, which saves
memory by having only one object per hash table entry instead of two
(each with their own Object overheads), and also by having a custome
hashCode() method that just uses the key value, instead of hashing
that key value.

knucleotide.clojure-6e-java9inspired-moreparallel.clojure
Like previous version, but breaks work up into smaller pieces,
e.g. instead of running one thread to calculate all of the hash table
for length 18 substrings, break that work up into separate threads
that each calculate a hash table for a subset of the input string,
then combine their results together at the end.

knucleotide.clojure-7.clojure
Like previous version, but uses medusa-pmap instead of pmap for higher
parallelism.

----------------------------------------------------------------------
x86 Ubuntu Intel Q6600 one core (32-bit 1 core)

                   CPU   Elapsed   Memory  Code   CPU Load
                   secs   secs     KB       B
Java 7 -server #9  33.80  33.91   340,296  2431    0%  0%  0% 100%
Java 7 -server #3  51.00  51.05   493,788  1630    0%  0%  0% 100%
Java 7 -server #2  51.49  51.55   494,388  1602    0%  0%  0% 100%
Clojure #6        129.11 129.27 1,007,132  1737    0%  0%  0% 100%
Clojure #5        221.57 221.81 1,009,640  1597    0%  0%  0% 100%
Clojure #4        224.13 224.69 1,003,200  1944    0%  0%  0% 100%
Clojure #2         7 min  7 min 1,009,736  1615    0%  0%  0% 100%
Clojure           13 min 13 min 1,006,944   809    0%  0%  0% 100%
Clojure #3        failed

knucleotide.clojure-6.clojure

MAKE:
mv knucleotide.clojure-6.clojure knucleotide.clj
/usr/local/src/jdk1.7.0_06/bin/java -Dclojure.compile.path=. -cp .:/usr/local/src/clojure-1.4.0/clojure-1.4.0-slim.jar: clojure.lang.Compile knucleotide

/usr/local/src/jdk1.7.0_06/bin/java -server -XX:+TieredCompilation -XX:+AggressiveOpts -Xmx1024m -cp .:/usr/local/src/clojure-1.4.0/clojure-1.4.0-slim.jar: knucleotide 0 < knucleotide-input25000000.txt

knucleotide.java-9.java

MAKE:
mv knucleotide.java-9.java knucleotide.java
/usr/local/src/jdk1.7.0_06/bin/javac knucleotide.java
Note: knucleotide.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
0.62s to complete and log all make actions

COMMAND LINE:
/usr/local/src/jdk1.7.0_06/bin/java -Xmx2048m -server -XX:+TieredCompilation -XX:+AggressiveOpts knucleotide 0 < knucleotide-input25000000.txt
----------------------------------------------------------------------
x86 Ubuntu Intel Q6600 quad-core (32-bit 4 core)

                   CPU   Elapsed   Memory  Code   CPU Load
                   secs   secs     KB       B
Java 7 -server #9  34.18   9.45   382,776  2431   88% 87% 93% 94%
Java 7 -server #3  45.65  13.13   492,936  1630   92% 82% 86% 89%
Java 7 -server #2  45.88  13.17   493,952  1602   81% 86% 84% 98%
Clojure #6        140.34  50.85 1,002,840  1737   60% 68% 61% 88%
Clojure #4        231.13  72.00 1,000,252  1944   72% 84% 83% 84%
Clojure #5        231.69  74.03   997,676  1597   96% 90% 64% 65%
Clojure #2        12 min 200.05 1,006,968  1615   91% 88% 89% 93%
Clojure           11 min  9 min 1,010,696   809    9% 15% 92%  9%
Clojure #3        failed

knucleotide.clojure-6.clojure

Same as 32-bit 1 core commands, except for this platform there was no
colon (:) after the clojure-1.4.0-slim.jar path name.

knucleotide.java-9.java

Same commands as 32-bit 1 core commands
----------------------------------------------------------------------
x64 Ubuntu Intel Q6600 one core (64-bit 1 core)

                   CPU   Elapsed   Memory  Code   CPU Load
                   secs   secs     KB       B
Java 7 -server #9  29.05  29.09   525,364  2431    0%  0%  0% 100%
Java 7 -server #3  50.83  50.89   436,988  1630    0%  0%  0% 100%
Java 7 -server #2  50.86  50.91   436,708  1602    0%  0%  0% 100%
Clojure #6        120.55 120.72   992,740  1737    0%  0%  0% 100%
Clojure #4        204.01 204.26   992,588  1944    0%  0%  0% 100%
Clojure #5        204.19 204.44   988,404  1597    0%  0%  0% 100%
Clojure #2         6 min  6 min   993,464  1615    0%  0%  0% 100%
Clojure            9 min  9 min   992,048   809    0%  0%  0% 100%
Clojure #3        failed

knucleotide.clojure-6.clojure

Same as 32-bit 1 core commands, except for this platform there was no
colon (:) after the clojure-1.4.0-slim.jar path name, and the "-slim"
was removed from all filenames that had them.

knucleotide.java-9.java

Same commands as 32-bit 1 core commands.
----------------------------------------------------------------------
x64 Ubuntu Intel Q6600 quad-core (64-bit 4 core)

                   CPU   Elapsed   Memory  Code   CPU Load
                   secs   secs     KB       B
Java 7 -server #9  29.10   7.85   509,076  2431   97% 92% 91% 93%
Java 7 -server #2  46.70  13.30   451,096  1602   87% 89% 93% 84%
Java 7 -server #3  46.64  13.36   448,276  1630   84% 85% 83% 98%
Clojure #6        135.20  48.89   997,100  1737   77% 46% 86% 68%
Clojure #4        222.82  68.24 1,001,544  1944   91% 77% 74% 86%
Clojure #5        250.41  75.64 1,000,592  1597   85% 76% 72% 74%
Clojure #2        12 min 211.07   996,320  1615   92% 90% 89% 95%
Clojure           15 min 10 min   999,928   809   59% 36% 20% 39%
Clojure #3        failed

knucleotide.clojure-6.clojure

Same commands as 64-bit 1 core commands.

knucleotide.java-9.java

Same commands as 32-bit 1 core commands
----------------------------------------------------------------------
