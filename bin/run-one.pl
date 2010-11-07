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
# An example use of the $FindBin::Bin string variable defined by the
# line above, to include the 'build' subdirectory beneath that in the
# list of directories searched when finding .pm files for later 'use'
# statements.  Useful if a .pm file is part of 'the whole package' you
# want to distribute with this script.
use lib "$FindBin::Bin" . "/build";
use IPC::Open3;
use Symbol qw(gensym);
use IO::File;


my $verbose = 0;

my $full_progname = $0;
my $progname = fileparse($full_progname);


my $os = `uname -o`;
chomp $os;
if ($verbose) {
    printf STDERR "\$os='%s'\n", $os;
}
my $os_full = `uname -a`;
chomp $os_full;


sub usage {
    print STDERR "usage: $progname [-h] [ file1 ... ]\n";
    print STDERR "
    -h            Show this help.
    -n            Print a 'header line' giving the name of each field
                  in the output CSV file, in addition to the
                  statistics line.
    -v            Enable debug messages.
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
  0.18,0.09,0.03,13688

Example of use on Windows XP + Cygwin:

  % ../bin/run-one.pl -i input\\medium-input.txt -o output\\medium-clj-1.2-output.txt \\Program\ Files\\Java\\jrmc-4.0.1-1.6.0\\bin\\java -version
  0.44,0.36,0.09,17080
";
}

my $opts = { };
getopts('hnvs:t:i:o:l:b:', $opts);

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

# TBD: The default path to timemem should be based on full path name,
# and where this script itself is installed.

my $timemem_cmd;
if ($os eq 'Cygwin') {
    $timemem_cmd = "..\\bin\\timemem";
} elsif ($os eq 'Darwin') {
    $timemem_cmd = "/usr/bin/time -lp";
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
    #my $tempdir = tempdir( CLEANUP => 1 );
    my $tempdir = tempdir( CLEANUP => 0 );
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
my $end_time = localtime();
seek CATCHERR, 0, 0;

#open(F,$cmd_to_run . "|") or die sprintf "Could not run command '%s'.  Aborting.\n", $cmd_to_run;

if ($os eq 'Cygwin') {
    my $found_pid = 0;
    while (<CATCHERR>) {
	chomp;
	s/\r$//;
	if (/^Process ID/) {
	    $found_pid = 1;
	} elsif ($found_pid) {
	    if (/^\s+elapsed time \(seconds\): (.*)\s*$/) {
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
	    } elsif (/^\s+(\d+)\s+maximum resident set size\s*$/) {
		$max_rss_kbytes = $1 / 1024;
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
                # Note: There is a bug in GNU time version 1.7 (see
                # which version you have using the command
                # '/usr/bin/time -V') which causes the maximum
                # resident set size to be 4 times too large.  See:
                # http://www.mail-archive.com/help-gnu-utils@gnu.org/msg01371.html
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
if ($opts->{n}) {
    printf "\"OS description\",\"Language implementation\",\"Benchmark name\",\"Source file name\",\"Start time\",\"End time\",\"Command measured\",elapsed_sec,user_sec,sys_sec,max_rss_kbytes\n";
}
printf "%s,", csv_str($os_full);
printf "%s,", csv_str($language_implementation_desc_str);
printf "%s,", csv_str($benchmark_name);
printf "%s,", csv_str($source_file_name);
printf "%s,", csv_str($start_time);
printf "%s,", csv_str($end_time);
printf "%s,", csv_str($cmd_to_time);
printf "%s,%s,%s,%s", $elapsed_sec, $user_sec, $sys_sec, $max_rss_kbytes;
# TBD: percent busy time for each cpu core
# TBD: percent of cpu this job got while it ran
# TBD: exit status
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

printf "\n";

exit 0;


sub csv_str {
    my $str = shift;
    my $csv_str;

    $csv_str = $str;
    $csv_str =~ s/"/""/g;
    return '"' . $csv_str . '"';
}
