As of May 28, 2012, the fastest Clojure programs for the revcomp
program were:

----------------------------------------------------------------------
x86 Ubuntu Intel Q6600 one core (32-bit 1 core)

               CPU   Elapsed   Memory  Code   CPU Load
               secs   secs     KB       B
Clojure        15.18  15.21   530,848   997    0%  0%  0% 100%
Java 7 -server  1.87   1.90   511,340   745    1%  1%  0% 100%

revcomp.clojure-4.clojure

mv revcomp.clojure-4.clojure revcomp.clj
/usr/local/src/jdk1.7.0_04/bin/java -Dclojure.compile.path=. -cp .:/usr/local/src/clojure-1.4.0/clojure-1.4.0-slim.jar: clojure.lang.Compile revcomp
Compiling revcomp to .
5.15s to complete and log all make actions

COMMAND LINE:
/usr/local/src/jdk1.7.0_04/bin/java -server -XX:+TieredCompilation -XX:+AggressiveOpts -Xmx384m -cp .:/usr/local/src/clojure-1.4.0/clojure-1.4.0-slim.jar: revcomp 0 < revcomp-input25000000.txt

revcomp.java-6.java

mv revcomp.java-6.java revcomp.java
/usr/local/src/jdk1.7.0_04/bin/javac revcomp.java
0.52s to complete and log all make actions

COMMAND LINE:
/usr/local/src/jdk1.7.0_04/bin/java  -server -XX:+TieredCompilation -XX:+AggressiveOpts revcomp 0 < revcomp-input25000000.txt
----------------------------------------------------------------------
x86 Ubuntu Intel Q6600 quad-core (32-bit 4 core)

               CPU   Elapsed   Memory  Code   CPU Load
               secs   secs     KB       B
Clojure        15.66   8.42   532,492   997   26% 38% 98% 26%
Java 7 -server  2.88   1.52   294,656  1661   56% 74% 46% 16%

revcomp.clojure-4.clojure

Same as 32-bit 1 core commands, except for this platform there was no
colon (:) after the clojure-1.4.0-slim.jar path name.

revcomp.java-3.java

Same commands as 32-bit 1 core commands except original source file
name is revcomp.java-3.java instead of revcomp.java-6.java

----------------------------------------------------------------------
x64 Ubuntu Intel Q6600 one core (64-bit 1 core)

               CPU   Elapsed   Memory  Code   CPU Load
               secs   secs     KB       B
Clojure        5.13   5.17    358,264   997    0%  0%  0% 100%
Java 7 -server 1.79   1.83    514,512   745    2%  1%  2% 100%

revcomp.clojure-4.clojure

Same as 32-bit 1 core commands, except for this platform there was no
colon (:) after the clojure-1.4.0-slim.jar path name, and the "-slim"
was removed from all filenames that had them.

revcomp.java-6.java

Same commands as 32-bit 1 core commands.

----------------------------------------------------------------------
x64 Ubuntu Intel Q6600 quad-core (64-bit 4 core)

               CPU   Elapsed   Memory  Code   CPU Load
               secs   secs     KB       B
Clojure        5.52   4.25    448,296   997   14%  9% 34% 75%
Java 7 -server 2.74   1.31    298,220  1661   45% 54% 37% 79%

revcomp.clojure-4.clojure

Same commands as 64-bit 1 core commands.

revcomp.java-3.java

Same commands as 32-bit 1 core commands except original source file
name is revcomp.java-3.java instead of revcomp.java-6.java
----------------------------------------------------------------------
