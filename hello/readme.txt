On Ubuntu 10.4 LTS Linux, with this version of Java:

# java -version
java version "1.6.0_22"
Java(TM) SE Runtime Environment (build 1.6.0_22-b04)
Java HotSpot(TM) Client VM (build 17.1-b03, mixed mode, sharing)

Here are the memory usages for a basic Hello, world! program with the
following languages:

----------------------------------------------------------------------
Clojure 1.2
----------------------------------------------------------------------

% ../bin/measureproc java -server -Xmx4m -classpath ~/lein/swank-clj-1.2.0/lib/clojure-1.2.0.jar:./obj/clj-1.2 hello
    Command measured          : java -server -Xmx4m -classpath /home/andy/lein/swank-clj-1.2.0/lib/clojure-1.2.0.jar:./obj/clj-1.2 hello
    Current working directory : /home/andy/git/clojure-benchmarks/hello
    Elapsed time (sec)        : 1.48
    User CPU time (sec)       : 1.49
    System CPU time (sec)     : 0.28
    Max resident set size (KB): 27084
    Start time                : Tue Nov 23 19:12:10 2010
    End time                  : Tue Nov 23 19:12:11 2010
    Per core CPU usage (2 cores): 87% 38%
    Exit status               : 0
    OS description            : Linux ubuntu 2.6.32-25-generic #45-Ubuntu SMP Sat Oct 16 19:48:22 UTC 2010 i686 GNU/Linux

% ../bin/measureproc --jvm-gc-stats java -server -Xmx4m -classpath $HOME/lein/swank-clj-1.2.0/lib/clojure-1.2.0/lib/clojure-1.2.0.jar:./obj/clj-1.2 hello

    Command measured          : java -Xloggc:/tmp/NG_bsNZwqT/4vJzCHnWJu.txt -server -Xmx4m -classpath /home/andy/lein/swank-clj-1.2.0/lib/clojure-1.2.0.jar:./obj/clj-1.2 hello
    Current working directory : /home/andy/git/clojure-benchmarks/hello
    Elapsed time (sec)        : 1.52
    User CPU time (sec)       : 1.46
    System CPU time (sec)     : 0.33
    Max resident set size (KB): 27520
    Start time                : Tue Nov 23 19:11:39 2010
    End time                  : Tue Nov 23 19:11:40 2010
    Per core CPU usage (2 cores): 76% 45%
    Exit status               : 0
    Number of GCs             : 17
    Total memory GCed (KB)    : 17678
    Total GC time (sec)       : 0.149717
    Maximum total available memory (KB): 3968
    Maximum total allocated memory (KB): 1847
    Maximum total live memory (KB)     : 805
    OS description            : Linux ubuntu 2.6.32-25-generic #45-Ubuntu SMP Sat Oct 16 19:48:22 UTC 2010 i686 GNU/Linux

----------------------------------------------------------------------
Java
----------------------------------------------------------------------
% ../bin/measureproc java -server -Xmx4m -classpath ./obj/java hello
    Command measured          : java -server -Xmx4m -classpath ./obj/java hello
    Current working directory : /home/andy/git/clojure-benchmarks/hello
    Elapsed time (sec)        : 0.35
    User CPU time (sec)       : 0.06
    System CPU time (sec)     : 0.04
    Max resident set size (KB): 10136
    Start time                : Tue Nov 23 19:12:52 2010
    End time                  : Tue Nov 23 19:12:53 2010
    Per core CPU usage (2 cores): 76% 19%
    Exit status               : 0
    OS description            : Linux ubuntu 2.6.32-25-generic #45-Ubuntu SMP Sat Oct 16 19:48:22 UTC 2010 i686 GNU/Linux

% ../bin/measureproc --jvm-gc-stats java -server -Xmx4m -classpath ./obj/java h>
    Command measured          : java -Xloggc:/tmp/TArtQ10Xwl/J5MS1jL_y8.txt -server -Xmx4m -classpath ./obj/java hello
    Current working directory : /home/andy/git/clojure-benchmarks/hello
    Elapsed time (sec)        : 0.14
    User CPU time (sec)       : 0.04
    System CPU time (sec)     : 0.06
    Max resident set size (KB): 10136
    Start time                : Tue Nov 23 19:13:14 2010
    End time                  : Tue Nov 23 19:13:14 2010
    Per core CPU usage (2 cores): 13% 59%
    Exit status               : 0
    Number of GCs             : 0
    Total memory GCed (KB)    : 0
    Total GC time (sec)       : 0
    Maximum total available memory (KB): 0
    Maximum total allocated memory (KB): 0
    Maximum total live memory (KB)     : 0
    OS description            : Linux ubuntu 2.6.32-25-generic #45-Ubuntu SMP Sat Oct 16 19:48:22 UTC 2010 i686 GNU/Linux


----------------------------------------------------------------------
JRuby
----------------------------------------------------------------------

% ../bin/measureproc /home/andy/sw/jruby-1.5.5/bin/jruby --server -J-Xmx4m	hello.jruby
    Command measured          : /home/andy/sw/jruby-1.5.5/bin/jruby --server -J-Xmx4m hello.jruby
    Current working directory : /home/andy/git/clojure-benchmarks/hello
    Elapsed time (sec)        : 0.82
    User CPU time (sec)       : 0.51
    System CPU time (sec)     : 0.35
    Max resident set size (KB): 24052
    Start time                : Tue Nov 23 20:27:57 2010
    End time                  : Tue Nov 23 20:27:58 2010
    Per core CPU usage (2 cores): 67% 38%
    Exit status               : 0
    OS description            : Linux ubuntu 2.6.32-25-generic #45-Ubuntu SMP Sat Oct 16 19:48:22 UTC 2010 i686 GNU/Linux

% ../bin/measureproc --jvm-gc-stats /home/andy/sw/jruby-1.5.5/bin/jruby --server -J-Xmx4m hello.jruby
    Command measured          : /home/andy/sw/jruby-1.5.5/bin/jruby -J-Xloggc:/tmp/iF0ipodAvG/NudkhLzU6Z.txt --server -J-Xmx4m hello.jruby
    Current working directory : /home/andy/git/clojure-benchmarks/hello
    Elapsed time (sec)        : 0.76
    User CPU time (sec)       : 0.57
    System CPU time (sec)     : 0.28
    Max resident set size (KB): 23928
    Start time                : Tue Nov 23 20:29:12 2010
    End time                  : Tue Nov 23 20:29:12 2010
    Per core CPU usage (2 cores): 35% 74%
    Exit status               : 0
    Number of GCs             : 3
    Total memory GCed (KB)    : 2581
    Total GC time (sec)       : 0.016581
    Maximum total available memory (KB): 3968
    Maximum total allocated memory (KB): 1510
    Maximum total live memory (KB)     : 673
    OS description            : Linux ubuntu 2.6.32-25-generic #45-Ubuntu SMP Sat Oct 16 19:48:22 UTC 2010 i686 GNU/Linux

----------------------------------------------------------------------
Scala
----------------------------------------------------------------------

% ../bin/measureproc java -server -Xmx4m -Xbootclasspath/a:/home/andy/sw/scala-2.8.1.final/lib/scala-library.jar:./obj/scala hello
    Command measured          : java -server -Xmx4m -Xbootclasspath/a:/home/andy/sw/scala-2.8.1.final/lib/scala-library.jar:./obj/scala hello
    Current working directory : /home/andy/git/clojure-benchmarks/hello
    Elapsed time (sec)        : 0.21
    User CPU time (sec)       : 0.11
    System CPU time (sec)     : 0.06
    Max resident set size (KB): 12240
    Start time                : Tue Nov 23 20:37:33 2010
    End time                  : Tue Nov 23 20:37:34 2010
    Per core CPU usage (2 cores): 56% 28%
    Exit status               : 0
    OS description            : Linux ubuntu 2.6.32-25-generic #45-Ubuntu SMP Sat Oct 16 19:48:22 UTC 2010 i686 GNU/Linux
andy@ubuntu ~/git/clojure-benchmarks/hello

% ../bin/measureproc --jvm-gc-stats java -server -Xmx4m -Xbootclasspath/a:/home/andy/sw/scala-2.8.1.final/lib/scala-library.jar:./obj/scala hello
    Command measured          : java -Xloggc:/tmp/iOMMiwzjOY/70Xi7vUo1G.txt -server -Xmx4m -Xbootclasspath/a:/home/andy/sw/scala-2.8.1.final/lib/scala-library.jar:./obj/scala hello
    Current working directory : /home/andy/git/clojure-benchmarks/hello
    Elapsed time (sec)        : 0.22
    User CPU time (sec)       : 0.09
    System CPU time (sec)     : 0.08
    Max resident set size (KB): 12232
    Start time                : Tue Nov 23 20:37:40 2010
    End time                  : Tue Nov 23 20:37:40 2010
    Per core CPU usage (2 cores): 71% 13%
    Exit status               : 0
    Number of GCs             : 0
    Total memory GCed (KB)    : 0
    Total GC time (sec)       : 0
    Maximum total available memory (KB): 0
    Maximum total allocated memory (KB): 0
    Maximum total live memory (KB)     : 0
    OS description            : Linux ubuntu 2.6.32-25-generic #45-Ubuntu SMP Sat Oct 16 19:48:22 UTC 2010 i686 GNU/Linux
