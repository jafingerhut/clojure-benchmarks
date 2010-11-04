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
