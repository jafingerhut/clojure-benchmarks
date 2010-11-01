Sun Oct 31 12:45:56 PDT 2010
Andy Fingerhut
andy_fingerhut@alum.wustl.edu


timemem.exe - Run a Win32 binary and measure elapsed, kernel, and
    user CPU times, and maximum working set size memory used
    during its entire execution, plus a few other statistics.

Examples command line and output:

    ----------------------------------------
    C:\> timemem "find \"e\" myfile.txt"
    
    ---------- MYFILE.TXT
    >ONE Homo sapiens alu
    >TWO IUB ambiguity codes
    >THREE Homo sapiens frequency
    
    Process ID: 476
        elapsed time (seconds): 5.81
        user time (seconds): 0.55
        kernel time (seconds): 0.30
        Page Fault Count: 3150
        Peak Working Set Size (kbytes): 12420
        Quota Peak Paged Pool Usage: 78324
        Quota Peak Non Paged Pool Usage: 2240
        Peak Pagefile Usage: 917504
    ----------------------------------------

What is this program for?  For those running on Windows that have
Win32 programs that run for a while, then stop.  You want to get
performance measurements for them, including one or more of the values
shown in the sample output above.  You want to do it from the command
line.  Why?  One reason might be so you can have a batch script that
records these measurements for many program invocations, without
having to watch the Windows Task Manager window while the program is
running.  You can make a batch file, start it, work on other things,
and look at the results later.

This is what /usr/bin/time on Linux and Mac OS X do, given suitable
command line options to print detailed measurements in their output
(see example output below).  Cygwin has a "time" package (in the
"Utils" category) that includes the command /usr/bin/time, which works
fine for measuring Cygwin programs.  However, although it prints
similar output when you use it to measure a Win32, i.e. non-Cygwin,
binary, what it is measuring in that case is not the Win32 program,
but a bash process created by Cygwin that then creates the Win32
process, but /usr/bin/time in that case is measuring performance for
the bash process, not the Win32 process.  See the following web pages
if you are curious about some of the gory details of why this happens:

    http://cygwin.ru/ml/cygwin/2001-09/msg00202.html
    http://cygwin.ru/ml/cygwin/2001-09/msg00205.html

Sample output from Mac OS X 10.5.8:

    ----------------------------------------
    % /usr/bin/time -lp /usr/bin/wc PS1-01.m4v 
     6166365 46283357 1532034853 PS1-01.m4v
    real        27.09
    user        24.65
    sys          1.17
       1433600  maximum resident set size
             0  average shared memory size
             0  average unshared data size
             0  average unshared stack size
           361  page reclaims
             1  page faults
             0  swaps
             0  block input operations
             0  block output operations
             0  messages sent
             0  messages received
             0  signals received
             4  voluntary context switches
         21604  involuntary context switches
    ----------------------------------------

Sample timemem.exe output from Windows XP Professional, Service Pack
3:

    ----------------------------------------
    C:\> timemem "find \"e\" myfile.txt"
    
    ---------- MYFILE.TXT
    >ONE Homo sapiens alu
    >TWO IUB ambiguity codes
    >THREE Homo sapiens frequency
    
    Process ID: 476
        elapsed time (seconds): 5.81
        user time (seconds): 0.55
        kernel time (seconds): 0.30
        Page Fault Count: 3150
        Peak Working Set Size (kbytes): 12420
        Quota Peak Paged Pool Usage: 78324
        Quota Peak Non Paged Pool Usage: 2240
        Peak Pagefile Usage: 917504
    ----------------------------------------

In my limited testing, I believe that the following values are always
the same on Windows XP SP3, at least.

    ============   =============
                   Win XP SP3
    timemem.exe    Windows Task
    output name    Manager
    ============   =============
    
    Process ID     PID
    ------------   -------------
                   CPU Time =
    user time      user time +
    kernel time    kernel time
    ------------   -------------
    Peak Working   Peak Mem
    Set Size       Usage
    (kbytes)
    ------------   -------------
    Page Fault     Page Faults?
    Count
    ------------   -------------
    Quota Peak
    Paged             ???
    Pool Usage
    ------------   -------------
    Quota Peak
    Non Paged         ???
    Pool Usage
    ------------   -------------
    Peak
    Pagefile          ???
    Usage
    ------------   -------------
        ???        VM Size
    ------------   -------------

If you can test this program on other versions of Windows and report
to me whether the measurements correspond to these or other similar
well-known performance measurements, I can add them to this
documentation.


Note that all output of timemem.exe is printed to the "standard error"
output stream, so if the program you are measuring prints its output
to "standard output" instead, then you can get the output of
timemem.exe saved into a file by itself with a command line like this:

    ----------------------------------------
    C:\> timemem "find \"e\" myfile.txt" 2> out.txt
    
    ---------- MYFILE.TXT
    >ONE Homo sapiens alu
    >TWO IUB ambiguity codes
    >THREE Homo sapiens frequency
    
    C:\> type out.txt
    
    Process ID: 476
        elapsed time (seconds): 5.81
        user time (seconds): 0.55
        kernel time (seconds): 0.30
        Page Fault Count: 3150
        Peak Working Set Size (kbytes): 12420
        Quota Peak Paged Pool Usage: 78324
        Quota Peak Non Paged Pool Usage: 2240
        Peak Pagefile Usage: 917504
    ----------------------------------------

Conversely, you can save standard output to a file while letting
standard error go to the screen like this:

    ----------------------------------------
    C:\> timemem "find \"e\" myfile.txt" > out2.txt
    
    Process ID: 476
        elapsed time (seconds): 5.81
        user time (seconds): 0.55
        kernel time (seconds): 0.30
        Page Fault Count: 3150
        Peak Working Set Size (kbytes): 12420
        Quota Peak Paged Pool Usage: 78324
        Quota Peak Non Paged Pool Usage: 2240
        Peak Pagefile Usage: 917504
    
    C:\> type out2.txt
    
    ---------- MYFILE.TXT
    >ONE Homo sapiens alu
    >TWO IUB ambiguity codes
    >THREE Homo sapiens frequency
    ----------------------------------------

You can even simultaneously save the standard output in one file, but
the standard error in a different one, like so:

    ----------------------------------------
    C:\> timemem "find \"e\" myfile.txt" > out3.txt 2>out4.txt
    ----------------------------------------

where in the last case, out3.txt would be filled with the standard
output from the measured command, and out4.txt would be filled with
the standard output from the measured command (if anything) plus the
output from timemem.exe.

timemem.exe's output should be printed only after the measured process
has exited, so you should always see its output after the output from
the measured process.  There may be cases in which a measured process
can cause this to not be true, however, that I am not yet aware of, so
don't rely on it unless you have good reason to know it is true.

----------------------------------------

WARNING: There is a way I could improve timemem.exe so that it does
not have this problem, but for now the problem exists and you should
know about it.  The problem arises when you want to use timemem.exe to
measure the performance of a program that is in a directory found in
your PATH environment variable _before_ the Windows system
directories, but another command with the same name is also in one ore
more of the Windows system directories.  In such cases, if you do not
give a full path name to the command you want to run, timemem.exe will
use the one in the Windows system directory, not the one that comes
earlier in your PATH, as you would likely expect and hope for.

My original motivation in creating timemem.exe was to run some
compiled Java programs under multiple different Java Virtual Machines,
and see what the run times and memory usages were, to compare them.  I
had multiple java.exe executables installed on my system in different
directories.  It seemed reasonable to me that whichever version of
java.exe was first in my PATH environment variable would be used by
timemem.exe, too, but that appears not to be the case.  Here is an
example:

    ----------------------------------------
    C:\> echo %PATH%
    C:\Program Files\Java\jrmc-4.0.1-1.6.0\bin;C:\Program Files\Windows Resource Kits\Tools\;C:\WINDOWS\system32;C:\WINDOWS;C:\WINDOWS\System32\Wbem;C:\Program Files\Steel Bank Common Lisp\1.0.37\ 
    ----------------------------------------

The following command is running the version of java.exe that is
installed in the first directory in my PATH, as expected, which is
Oracle JRockit:

    ----------------------------------------
    C:\> java -version
    java version "1.6.0_20"
    Java(TM) SE Runtime Environment (build 1.6.0_20-b02)
    Oracle JRockit(R) (build R28.0.1-21-133393-1.6.0_20-20100512-2132-windows-ia32, compiled mode)
    ----------------------------------------

However, when I run the same command via timemem, it is executing a
REM different java.exe

    ----------------------------------------
    C:\> timemem "java -version"
    java version "1.6.0_22"
    Java(TM) SE Runtime Environment (build 1.6.0_22-b04)
    Java HotSpot(TM) Client VM (build 17.1-b03, mixed mode, sharing)

    Process ID: 3924
        elapsed time (seconds): 0.20
        user time (seconds): 0.06
        kernel time (seconds): 0.03
        Page Fault Count: 1779
        Peak Working Set Size (kbytes): 6996
        Quota Peak Paged Pool Usage: 59836
        Quota Peak Non Paged Pool Usage: 3360
        Peak Pagefile Usage: 40075264
    ----------------------------------------

As a workaround, you can give a full path name to the command you want
to run on the timemem.exe command line.  For example:

    ----------------------------------------
    C:\> timemem "C:\Program Files\Java\jrmc-4.0.1-1.6.0\bin\java -version"
    java version "1.6.0_20"
    Java(TM) SE Runtime Environment (build 1.6.0_20-b02)
    Oracle JRockit(R) (build R28.0.1-21-133393-1.6.0_20-20100512-2132-windows-ia32, compiled mode)
    ----------------------------------------

I believe this happens because of the way I am calling
CreateProcess(), with NULL for the first argument and the
user-provided command line for the second.  According to the
CreateProcess() documentation at the location below, it appears that
the command name is first searched for in the 32-bit Windows system
directory, then the 16-bit Windows system directory, and a few other
places, before finally looking in the PATH environment variable.  It
would be nicer for timemem.exe if it always searched the PATH
environment variable first, or had an option to do so, but I don't see
one.

    http://msdn.microsoft.com/en-us/library/ms682425%28VS.85%29.aspx

My best idea for how to make timemem.exe behave in a less surprising
fashion is to change timemem.exe so that it by itself looks through
the command PATH for the command name, and then in its CreateProcess()
call gives the full path name to the command as the first argument,
assuming it finds one.  I'm hoping there is some Win32 API call I can
make to do this for me, but it isn't difficult code to write if no
such thing exists.

----------------------------------------

Why the name timemem.exe?  There are already several programs that
have been called time.exe on Windows, and I did not want to use the
same name.  Other names like wintime.exe, timem.exe, etc. returned
results from Google searches for things that already existed, and in
some cases were signs of virus infections.  timemem.exe is intended to
be an abbreviation for "TIME and MEMory", is reasonably short, and I
could find no prominent examples of existing programs on Windows with
the same name in a few Google searches as of October 2010.

======================================================================

When I compiled timem-mingw.c under MinGW and ran it from a
DOS/Windows cmd window, or from a .bat file, and used it to execute
and time a non-Cygwin binary, i.e. a "normal Win32 binary", I was able
to get the kind of performance measurements I expected to see from it,
i.e. the numbers matched up with the kinds of measurements I was
seeing when monitoring the Windows Task Manager "by hand".

I do not know why, but even after several variations of different
command lines, I was not able to find a way to run this timem.exe from
a Cygwin bash shell, and measure the performance of a non-Cygwin
binary, i.e. a normal Win32 binary.  No big deal, since I can work
around that using a .bat file, but it would be nice if there was a way
to do it.

Note that for getting these kinds of performance measurements for
Cygwin binaries, you can do that using a command like this from a
Cygwin bash shell window:

    /usr/bin/time -v <cygwin command line>

However, as noted above, this only gives reasonable measurement values
for Cygwin binaries, not Win32 binaries.
