Thu Nov  4 11:27:50 PDT 2010
Andy Fingerhut
andy_fingerhut@alum.wustl.edu

Notes on programs that measure time and memory use of other programs,
and notes on programs I want to create to automate the measurement
process of many benchmark program runs.


Make one (or several different) programs that can be invoked to do
these things:

(1) Given a full cmd line, run it once, and output performance
measurements in a _common_ format across all of these platforms:

    Ubuntu 10.4
    Mac OS X 10.5.8
    Mac OS X 10.6.4
    Windows XP SP3

Existing formats:

----- Mac OS X 10.5.8 -----------------------------------
% /usr/bin/time -l wc p525-clean.jpg 
   9759   42339 2037699 p525-clean.jpg
        1.03 real         0.93 user         0.01 sys
    790528  maximum resident set size  <-- NOTE: Units is in BYTES
         0  average shared memory size
         0  average unshared data size
         0  average unshared stack size
       199  page reclaims
        12  page faults
         0  swaps
         0  block input operations
         3  block output operations
         0  messages sent
         0  messages received
         0  signals received
        18  voluntary context switches
       616  involuntary context switches
----- Ubuntu 10.4 Linux -----------------------------------
% uname -a
Linux ubuntu 2.6.32-25-generic #44-Ubuntu SMP Fri Sep 17 20:26:08 UTC 2010 i686 GNU/Linux

% /usr/bin/time -v ./test-memuse 51200
Attempting to allocate 51200 kbytes of memory...
Succeeded.  Attempting to initialize it all...
Initialization complete.  Sleeping forever.  You'll have to kill me now.
Command terminated by signal 15
	Command being timed: "./test-memuse 51200"
	User time (seconds): 0.04
	System time (seconds): 0.28
	Percent of CPU this job got: 3%
	Elapsed (wall clock) time (h:mm:ss or m:ss): 0:09.49
	Average shared text size (kbytes): 0
	Average unshared data size (kbytes): 0
	Average stack size (kbytes): 0
	Average total size (kbytes): 0
	Maximum resident set size (kbytes): 206496  <-- NOTE: GNU time 1.7 has a bug where this number is 4 times more than it should be.  Divide it by 4 to get the correct answer in units of kbytes.  See http://www.mail-archive.com/help-gnu-utils@gnu.org/msg01371.html
	Average resident set size (kbytes): 0
	Major (requiring I/O) page faults: 0
	Minor (reclaiming a frame) page faults: 12949
	Voluntary context switches: 11
	Involuntary context switches: 3
	Swaps: 0
	File system inputs: 0
	File system outputs: 0
	Socket messages sent: 0
	Socket messages received: 0
	Signals delivered: 0
	Page size (bytes): 4096
	Exit status: 0
----- Windows XP SP3 + Cygwin -----------------------------------
Admin@john-win ~/git/clojure-benchmarks/memuse
% ../bin/timemem.exe './test-memuse-mingw.exe 51200'
Attempting to allocate 51200 kbytes of memory...
Succeeded.  Attempting to initialize it all...
Initialization complete.  Exiting.

Process ID: 3524
    elapsed time (seconds): 0.14
    user time (seconds): 0.13
    kernel time (seconds): 0.03
    Page Fault Count: 13020
    Peak Working Set Size (kbytes): 52160   <-- NOTE: Units are kbytes.
    Quota Peak Paged Pool Usage: 12372
    Quota Peak Non Paged Pool Usage: 1208
    Peak Pagefile Usage: 52764672

The above works directly from a Cygwin bash shell, but this does not:

Admin@john-win ~/git/clojure-benchmarks/knuc
% /cygdrive/c/Program\ Files/Java/jrmc-4.0.1-1.6.0/bin/java -version
java version "1.6.0_20"
Java(TM) SE Runtime Environment (build 1.6.0_20-b02)
Oracle JRockit(R) (build R28.0.1-21-133393-1.6.0_20-20100512-2132-windows-ia32, compiled mode)
Admin@john-win ~/git/clojure-benchmarks/knuc
% ../bin/timemem.exe '/cygdrive/c/Program\ Files/Java/jrmc-4.0.1-1.6.0/bin/java -version'
CreateProcess failed (3).
Exit 26

Running it from a batch file does work.  Unless I find something less
kludgy that works, I will use this method.

In preparation, you must do these things in a Cygwin bash shell:

% cd clojure-benchmarks
% ./init.sh output
% cd knuc
% rm input/medium-input.txt
% cp ../fasta/output/medium-expected-output.txt input/medium-input.txt
% ./clj-compile.sh clj-1.2
% cat clj-12-med.bat
..\bin\timemem.exe "\Program Files\Java\jrmc-4.0.1-1.6.0\bin\java -server -Xmx1536m -classpath \cygwin\home\Admin\lein\swank-clj-1.2.0\lib\clojure-1.2.0.jar;.\obj\clj-1.2 knucleotide" < input\medium-input.txt > output\medium-clj-1.2-output.txt
% ./clj-12-med.bat

C:\cygwin\home\Admin\git\clojure-benchmarks\knuc>..\bin\timemem.exe "\Program Files\Java\jrmc-4.0.1-1.6.0\bin\java -server -Xmx1536m -classpath \cygwin\home\Admin\lein\swank-clj-1.2.0\lib\clojure-1.2.0.jar;.\obj\clj-1.2 knucleotide"  0<input\medium-input.txt 1>output\medium-clj-1.2-output.txt 
Starting part 0
Starting part 6
Starting part 1
Starting part 5
Finished part 0
Finished part 1
Starting part 2
Finished part 5
Starting part 3
Finished part 2
Finished part 3
Finished part 6
Starting part 4
Finished part 4

Process ID: 2868
    elapsed time (seconds): 16.61
    user time (seconds): 25.63
    kernel time (seconds): 0.30
    Page Fault Count: 54231
    Peak Working Set Size (kbytes): 110264
    Quota Peak Paged Pool Usage: 78332
    Quota Peak Non Paged Pool Usage: 23320
    Peak Pagefile Usage: 172056576
Exit 1  Admin@john-win ~/git/clojure-benchmarks/knuc

----------------------------------------

(2) Given a full cmd line, run it a minimum of 4 times (configurable,
with 4 as default), and a maximum of 10 times (again a default value),
measuring the elapsed and total CPU times of each run.  Stop when some
measure of the variance is "small enough".

It should have an option to run it only once, too, for CLI use.

(3) Given (2), another program should use binary search to find the
least memory required to run the program and get the expected output.
Then it should run the program multiple times with that memory limit,
then that plus 8 MBytes, that plus 16 Mbytes, etc. (or an option to
use a different delta than 8 MBytes), and stop when the run time is at
least .95 times as large as the run time of the next lowest memory
limit tried.
