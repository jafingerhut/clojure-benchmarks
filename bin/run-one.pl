: # use perl -*-Perl-*-
eval 'exec perl -S "$0" ${1+"$@"}'
    if 0;
# -*cperl-*-

use strict;
use Getopt::Std;
use File::Basename;
# A library for creating temporary files
use File::Temp qw/ tempfile tempdir /;
# A library to find the directory in which the script is installed.
# This can be useful for making it easy to install other auxiliary
# files needed by the script all in one place, and no matter where
# someone installs 'the whole package' of files, the script can find
# them, without needing hand-editing.
use FindBin;
use IPC::Open3;
use Symbol qw(gensym);
use IO::File;


my $verbose = 0;

my $full_progname = $0;
my $progname = fileparse($full_progname);


my $os = `uname -o`;
chomp $os;
my $os_full = `uname -a`;
chomp $os_full;


sub usage {
    print STDERR "usage: $progname [-h] [ file1 ... ]\n";
    print STDERR "
    -h            Show this help.
    -v            Enable debug messages.
    -c            Print output in CSV format, not really intended for
                  human consumption.
    -n            If -c is given, then also print a 'header line'
                  giving the name of each field in the output CSV
                  file, in addition to the statistics line.
    -s <os_name>  Can be used to specify one of the OS's: Cygwin,
                  Darwin, GNU/Linux
    -t <time_cmd> to specify the command to use to measure the running
                  time and memory usage of the process.
    -i <input_file>
    -o <output_file>
    -l <language_implementation_description_string>
    -b <benchmark_problem_name>

Example of use on Linux or Mac OS X:

  % ../bin/run-one.pl -i input/medium-input.txt -o output/medium-clj-1.2-output.txt java -version
  [ ... output removed ... ]

  % ../bin/run-one.pl -i input/medium-input.txt -o output/medium-clj-1.2-output.txt java -server -Xmx1536m -classpath ~/lein/swank-clj-1.2.0/lib/clojure-1.2.0.jar:./obj/clj-1.2 knucleotide
      Command measured          : java -server -Xmx1536m -classpath /Users/andy/lein/swank-clj-1.2.0/lib/clojure-1.2.0.jar:./obj/clj-1.2 knucleotide
      Elapsed time (sec)        : 82.35
      User CPU time (sec)       : 26.34
      System CPU time (sec)     : 0.65
      Max resident set size (kb): 0
      Start time                : Sun Nov  7 20:01:20 2010
      End time                  : Sun Nov  7 20:02:42 2010
      Exit status               : 0
      OS description            : Darwin our-imac.local 10.4.0 Darwin Kernel Version 10.4.0: Fri Apr 23 18:28:53 PDT 2010; root:xnu-1504.7.4~1/RELEASE_I386 i386 i386 iMac6,1 Darwin

Examples of use on Windows XP + Cygwin:

  % ../bin/run-one.pl -i input\\medium-input.txt -o output\\medium-clj-1.2-output.txt \\Program\ Files\\Java\\jrmc-4.0.1-1.6.0\\bin\\java -version
  [ ... output removed ... ]

  % ../bin/run-one.pl -v -i input\\quick-input.txt -o output\\quick-clj-1.2-output.txt \\Program\ Files\\Java\\jrmc-4.0.1-1.6.0\\bin\\java -server -Xmx1536m -classpath \"\\cygwin\\home\\Admin\\lein\\swank-clj-1.2.0\\lib\\clojure-1.2.0.jar;.\\obj\\clj-1.2\" knucleotide
  [ ... output removed ... ]
";
}

my $opts = { };
getopts('hvcns:t:i:o:l:b:', $opts);

if ($opts->{h}) {
    usage();
    exit(0);
}

if ($opts->{v}) {
    $verbose = 1;
}
if ($opts->{s}) {
    $os = $opts->{s};
}
if ($verbose) {
    printf STDERR "\$os='%s'\n", $os;
    printf STDERR "install dir='%s'\n", $FindBin::Bin;
}

# TBD: The default path to timemem should be based on full path name,
# and where this script itself is installed.

my $timemem_cmd;
if ($os eq 'Cygwin') {
    $timemem_cmd = "..\\bin\\timemem";
} elsif ($os eq 'Darwin') {
    #$timemem_cmd = "/usr/bin/time -lp";
    $timemem_cmd = "../bin/timemem-darwin";
} elsif ($os eq 'GNU/Linux') {
    $timemem_cmd = "/usr/bin/time -v";
} else {
    printf STDERR "Unknown OS string '%s'.  Only 'Cygwin', 'Darwin', and 'GNU/Linux' are supported.  Aborting.";
    exit 1;
}

if ($opts->{t}) {
    $timemem_cmd = $opts->{t};
}
my $input_file;
if ($opts->{i}) {
    $input_file = $opts->{i};
}
my $output_file;
if ($opts->{o}) {
    $output_file = $opts->{o};
}
my $language_implementation_desc_str = '';
if ($opts->{l}) {
    $language_implementation_desc_str = $opts->{l};
}
my $benchmark_name = '';
if ($opts->{b}) {
    $benchmark_name = $opts->{b};
}
my $source_file_name = '';


if ($#ARGV < 0) {
    printf STDERR "No command given.\n";
    usage();
    exit 1;
}
my $cmd_to_time = join(' ', @ARGV);
if ($verbose) {
    printf STDERR "\$cmd_to_time='%s'\n", $cmd_to_time;
}

my $cmd_str = '';
my $cmd_begin = '';
my $cmd_end = '';
if ($os eq 'Cygwin') {
    $cmd_begin = "\"";
    $cmd_end = "\"";
}

$cmd_str .= sprintf "%s %s%s%s", $timemem_cmd, $cmd_begin, $cmd_to_time, $cmd_end;
if (defined($input_file)) {
    $cmd_str .= sprintf " < %s", $input_file;
}
if (defined($output_file)) {
    $cmd_str .= sprintf " > %s", $output_file;
}
if ($verbose) {
    printf STDERR "\$cmd_str='%s'\n", $cmd_str;
}

my $cmd_to_run;
if ($os eq 'Cygwin') {
    # On Cygwin, put the command into a BAT file for execution.
    my $tempdir = tempdir( CLEANUP => 1 );
    #my $tempdir = tempdir( CLEANUP => 0 );
    my ($fh, $batchfile) = tempfile( DIR => $tempdir, SUFFIX => '.bat' );
    if ($verbose) {
	printf STDERR "\$tempdir='%s'\n", $tempdir;
    }
    printf $fh "%s\n", $cmd_str;
    close $fh;
    chmod 0755, $batchfile;
    $cmd_to_run = $batchfile;
} else {
    $cmd_to_run = $cmd_str;
}
if ($verbose) {
    printf STDERR "\$cmd_to_run='%s'\n", $cmd_to_run;
}


my ($elapsed_sec, $user_sec, $sys_sec, $max_rss_kbytes);
# I want to run the program and read its stderr output.  I'll assume
# that stdout is already being saved to a file, and ignore anything
# that goes to stdout.

# This is a slight modification of some sample code I found at:
# http://perldoc.perl.org/perlfaq8.html#How-can-I-capture-STDERR-from-an-external-command?

local *CATCHERR = IO::File->new_tmpfile;
my $start_time = localtime();
my $pid = open3(gensym, \*CATCHOUT, ">&CATCHERR", $cmd_to_run);
while (<CATCHOUT>) {
}
waitpid($pid, 0);
my $child_exit_status = $? >> 8;
my $end_time = localtime();
seek CATCHERR, 0, 0;

#open(F,$cmd_to_run . "|") or die sprintf "Could not run command '%s'.  Aborting.\n", $cmd_to_run;

my $cmd_exit_status;
if ($os eq 'Cygwin') {
    my $found_pid = 0;
    while (<CATCHERR>) {
	chomp;
	s/\r$//;
	if (/^Process ID/) {
	    $found_pid = 1;
	} elsif ($found_pid) {
	    if (/^\s+exit code: (.*)\s*$/) {
		$cmd_exit_status = $1;
	    } elsif (/^\s+elapsed time \(seconds\): (.*)\s*$/) {
		$elapsed_sec = $1;
	    } elsif (/^\s+user time \(seconds\): (.*)\s*$/) {
		$user_sec = $1;
	    } elsif (/^\s+kernel time \(seconds\): (.*)\s*$/) {
		$sys_sec = $1;
	    } elsif (/^\s+Peak Working Set Size \(kbytes\): (.*)\s*$/) {
		$max_rss_kbytes = $1;
	    } else {
		# Ignore the others
	    }
	} else {
	    # This is a line of output from the program, most likely
	    # one printed to stderr if stdout has been redirected.
	    # Ignore it for now.
	}
    }
} elsif ($os eq 'Darwin') {
    $cmd_exit_status = $child_exit_status;
    my $found_real = 0;
    while (<CATCHERR>) {
	chomp;
	if (/^\s*real\s+(.*)\s*$/) {
	    $elapsed_sec = $1;
	    $found_real = 1;
	} elsif ($found_real) {
	    if (/^\s*user\s+(.*)\s*$/) {
		$user_sec = $1;
	    } elsif (/^\s*sys\s+(.*)\s*$/) {
		$sys_sec = $1;
	    } elsif (/^\s*(\d+)\s+maximum resident set size\s*$/) {
		# Mac OS X reports max RSS in units of bytes.  Convert
		# to kbytes.
		$max_rss_kbytes = big_int_string_to_float($1) / 1024;
	    } else {
		# Ignore the others
	    }
	} else {
	    # This is a line of output from the program, most likely
	    # one printed to stderr if stdout has been redirected.
	    # Ignore it for now.
	}
    }
} elsif ($os eq 'GNU/Linux') {
    $cmd_exit_status = $child_exit_status;
    my $found_user = 0;
    while (<CATCHERR>) {
	chomp;
	if (/^\s*User time \(seconds\):\s*(.*)\s*$/) {
	    $user_sec = $1;
	    $found_user = 1;
	} elsif ($found_user) {
            if (/^\s*System time \(seconds\):\s*(.*)\s*$/) {
		$sys_sec = $1;
            } elsif (/^\s*Elapsed \(wall clock\) time \(h:mm:ss or m:ss\):\s*(.*)\s*$/) {
                my $tmp = $1;
                if ($tmp =~ /^(\d+):(\d+):(\d+(\.\d*))$/) {
                    my ($h, $m, $s) = ($1, $2, $3);
                    $elapsed_sec = $h * 3600 + $m * 60 + $s;
                } elsif ($tmp =~ /^(\d+):(\d+(\.\d*))$/) {
                    my ($m, $s) = ($1, $2);
                    $elapsed_sec = $m * 60 + $s;
                } else {
                    die sprintf "Unrecognized Elapsed (wall clock) time format seen.  Aborting:\n%s", $tmp;
                }
	    } elsif (/^\s*Maximum resident set size \(kbytes\):\s*(\d+)\s*$/) {
                # There is a bug in GNU time version 1.7 that causes
                # the maximum resident set size reported to be 4 times
                # too large.  See:

                # http://www.mail-archive.com/help-gnu-utils@gnu.org/msg01371.html
		#
		# You can find out which version of GNU time you have
		# in Cygwin using this command:
		#
                #     /usr/bin/time -V
		#
		# If that gives an error message like 'No such file or
		# directory', then you must use the Cygwin Setup
		# program to add the 'time' package.  You can search
		# for it by name, or look for it in the 'Utils'
		# category.
		$max_rss_kbytes = $1 / 4;
	    } else {
		# Ignore the others
	    }
	} else {
	    # This is a line of output from the program, most likely
	    # one printed to stderr if stdout has been redirected.
	    # Ignore it for now.
	}
    }
}
close(F);
if ($verbose) {
    printf STDERR "\$elapsed_sec='%s'\n", $elapsed_sec;
    printf STDERR "\$user_sec='%s'\n", $user_sec;
    printf STDERR "\$sys_sec='%s'\n", $sys_sec;
    printf STDERR "\$max_rss_kbytes='%s'\n", $max_rss_kbytes;
}
if ($opts->{c}) {
    if ($opts->{n}) {
	# TBD: Update this to match the order below
	printf "\"OS description\",\"Language implementation\",\"Benchmark name\",\"Source file name\",\"Start time\",\"End time\",\"Command measured\",elapsed_sec,user_sec,sys_sec,max_rss_kbytes\n";
    }
    printf "%s", csv_str($benchmark_name);
    printf ",%s", csv_str($language_implementation_desc_str);
    printf ",%s", csv_str($source_file_name);
    printf ",%s", csv_str($cmd_to_time);
    printf ",%s", $elapsed_sec;
    printf ",%s", $user_sec;
    printf ",%s", $sys_sec;
    printf ",%s", $max_rss_kbytes;
    printf ",%s", csv_str($start_time);
    printf ",%s", csv_str($end_time);
    # TBD: percent busy time for each cpu core
    # TBD: percent of cpu this job got while it ran
    printf ",%s", $cmd_exit_status;
    # TBD:
    #    description string for result summary:
    #        normal completion, output correct
    #        normal completion, output incorrect
    #	completed with exception <name>
    #        unknown type of completion, output incorrect
    #        killed after taking too long
    #    tbd: some measure of the maximum memory ever used by all
    #        simultaneously live Java objects, in kbytes (tbd: check out
    #        -verbose:gc output, and learn how to interpret the values to
    #        see if they can provide this measurement)
    # TBD: Description of hardware, e.g. # of CPU cores, type, and
    #     clock speed of each.

    # Mac OS X command to get CPU details:
    # /usr/sbin/system_profiler -detailLevel full SPHardwareDataType

    # Command to get OS details in a format more like in "About This
    # Mac" menu item:
    # sw_vers

    # Other similar commands mentioned at:
    # http://serverfault.com/questions/14981/getting-cpu-information-from-command-line-in-mac-os-x-server

    printf ",%s", csv_str($os_full);
    printf "\n";
} else {
    if ($benchmark_name ne '') {
	printf "    Benchmark name: %s\n", $benchmark_name;
    }
    if ($language_implementation_desc_str ne '') {
	printf "    Language implementation: %s\n", $language_implementation_desc_str;
    }
    if ($source_file_name ne '') {
	printf "    Source file name: %s\n", $source_file_name;
    }
    printf "    Command measured          : %s\n", $cmd_to_time;
    printf "    Elapsed time (sec)        : %s\n", $elapsed_sec;
    printf "    User CPU time (sec)       : %s\n", $user_sec;
    printf "    System CPU time (sec)     : %s\n", $sys_sec;
    printf "    Max resident set size (kb): %s\n", $max_rss_kbytes;
    printf "    Start time                : %s\n", $start_time;
    printf "    End time                  : %s\n", $end_time;
    printf "    Exit status               : %s\n", $cmd_exit_status;
    printf "    OS description            : %s\n", $os_full;
}

exit 0;


# Referring to the "Specification" section of this Wikipedia page:

# http://en.wikipedia.org/wiki/Comma-separated_values

# especially the "Basic rules" part of that section, it appears to me
# that I should be able to take _any_ string I want to put in a CSV
# field, whether it has quotes, commas, semicolons, etc. or not, put
# enclose it in double quotes, and replace all double quotes within
# the string with two consecutive double quotes, and it should be
# valid.  If I'm misunderstanding that, then I should be able to fix
# it by modifying this function.

sub csv_str {
    my $str = shift;
    my $csv_str;

    $csv_str = $str;
    $csv_str =~ s/"/""/g;
    return '"' . $csv_str . '"';
}


sub big_int_string_to_float {
    my $str = shift;

    #printf STDERR "jaf-debug: \$str=%s\n", $str;
    my $i;
    my $ret = 0.0;
    for ($i = 0; $i < length($str); $i++) {
	$ret = 10.0 * $ret + (ord(substr($str, $i, 1)) - ord('0'));
	#printf STDERR "jaf-debug: \$i=%d \$ret=%s\n", $i, $ret;
    }
    return $ret;
}
