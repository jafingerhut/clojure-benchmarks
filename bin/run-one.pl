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
use Math::BigInt;


my $verbose = 0;

my $full_progname = $0;
my $progname = fileparse($full_progname);


my $os = `uname -o`;
chomp $os;
my $os_full = `uname -a`;
chomp $os_full;

my $install_dir = $FindBin::Bin;
my $timemem_cmd = time_cmd_location($os, $install_dir);


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
                  Darwin, GNU/Linux.  This command line option need
                  only be used to override the default of '$os', which
                  is detected via the output of the 'uname -o'
                  command.
    -t <time_cmd> to specify the command to use to measure the running
                  time and memory usage of the process.  This is only
                  needed if you wish to override the default command,
                  which for platform '$os' is:
                  $timemem_cmd
    -i <input_file>
    -o <output_file>
    TBD: -? <check_output_cmd>
                  A command string used to check whether the output
                  file's contents are correct.  This command should
                  have a '%o' in it where the output file name should
                  be, and $progname will replace that with the output
                  file name.
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

  % ../bin/run-one.pl -v -i input\\\\quick-input.txt -o output\\\\quick-clj-1.2-output.txt \\\\Program\\ Files\\\\Java\\\\jrmc-4.0.1-1.6.0\\\\bin\\\\java -server -Xmx1536m -classpath \"\\\\cygwin\\\\home\\\\Admin\\\\lein\\\\swank-clj-1.2.0\\\\lib\\\\clojure-1.2.0.jar;.\\\\obj\\\\clj-1.2\" knucleotide
  [ ... output removed ... ]
";
}

my $opts = { };
getopts('hvcns:t:i:o:l:b:', $opts);

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

if ($opts->{h}) {
    usage();
    exit(0);
}

$timemem_cmd = time_cmd_location($os, $install_dir);

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
my $num_cpus;
my $usage_per_cpu = 'not calculated';
my ($per_cpu_stats_start, $total_cpu_stats_start);
my ($per_cpu_stats_end, $total_cpu_stats_end);

# I want to run the program and read its stderr output.  I'll assume
# that stdout is already being saved to a file, and ignore anything
# that goes to stdout.

# This is a slight modification of some sample code I found at:
# http://perldoc.perl.org/perlfaq8.html#How-can-I-capture-STDERR-from-an-external-command?

local *CATCHERR = IO::File->new_tmpfile;
if (($os eq 'GNU/Linux') || ($os eq 'Cygwin')) {
    ($per_cpu_stats_start, $total_cpu_stats_start) = linux_get_cpu_usage();
}
my $start_time = localtime();
my $pid = open3(gensym, \*CATCHOUT, ">&CATCHERR", $cmd_to_run);
while (<CATCHOUT>) {
}
waitpid($pid, 0);
my $child_exit_status = $? >> 8;
my $end_time = localtime();
if (($os eq 'GNU/Linux') || ($os eq 'Cygwin')) {
    ($per_cpu_stats_end, $total_cpu_stats_end) = linux_get_cpu_usage();
}
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
	    } elsif (/^\s+Peak Working Set Size \(kbytes\): (\d+)\s*$/) {
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
		# to kbytes.  Use BigInt arithmetic, in case the
		# integer is very large.
		my $max_rss_kbytes_bi = Math::BigInt->new($1);
		# Add 1023 before dividing (with truncation) by 1024,
		# so that the net effect is to round the number of
		# bytes up to the next whole number of kbytes.
		$max_rss_kbytes_bi->badd(1023);
		$max_rss_kbytes_bi->bdiv(1024);
		$max_rss_kbytes = sprintf "%s", $max_rss_kbytes_bi;
	    } elsif (/^\s*Per core CPU utilization \((\d+) cores\): (.*)$/) {
		($num_cpus, $usage_per_cpu) = ($1, $2);
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

		# Use BigInt arithmetic, in case the integer is very
		# large.
		my $max_rss_kbytes_bi = Math::BigInt->new($1);
		$max_rss_kbytes_bi->bdiv(4);
		$max_rss_kbytes = sprintf "%s", $max_rss_kbytes_bi;
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
if (($os eq 'GNU/Linux') || ($os eq 'Cygwin')) {
   $num_cpus = $#{$per_cpu_stats_start} + 1;
   if ($verbose) {
       printf STDERR "\$num_cpus=%s\n", $num_cpus;
   }
   $usage_per_cpu = '';
   my $i;
   for ($i = 0; $i < $num_cpus; $i++) {
       my $cpu_busy_percent =
	   cpu_busy_percent($per_cpu_stats_start->[$i]{idle},
			    $per_cpu_stats_end->[$i]{idle},
			    $per_cpu_stats_start->[$i]{total},
			    $per_cpu_stats_end->[$i]{total});
       my $cpu_busy_percent_without_bigints =
	   cpu_busy_percent_without_bigints($per_cpu_stats_start->[$i]{idle},
					    $per_cpu_stats_end->[$i]{idle},
					    $per_cpu_stats_start->[$i]{total},
					    $per_cpu_stats_end->[$i]{total});
       if ($cpu_busy_percent ne $cpu_busy_percent_without_bigints) {
	   die sprintf "For idle times %s %s and total times %s %s cpu_busy_percent() returned %s but cpu_busy_percent_without_bigints() returned %s\n",
	       $per_cpu_stats_start->[$i]{idle},
	       $per_cpu_stats_end->[$i]{idle},
	       $per_cpu_stats_start->[$i]{total},
	       $per_cpu_stats_end->[$i]{total},
	       $cpu_busy_percent,
	       $cpu_busy_percent_without_bigints;
       }
       $usage_per_cpu .= sprintf " %d%%", $cpu_busy_percent;
   }
   # Remove leading space.
   $usage_per_cpu = substr($usage_per_cpu, 1);
}
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
    printf ",%s", csv_str($usage_per_cpu);
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
    printf "    Per core CPU usage (%d cores): %s\n", $num_cpus, $usage_per_cpu;
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


sub linux_get_cpu_usage {
    my $per_cpu_stats = [];
    my $total_cpu_stats = {};

    my $filename = '/proc/stat';
    open(F,"$filename") or die sprintf "Could not open file '%s' for reading.\n";
    my $line = <F>;
    if ($line =~ /^\s*cpu\s+(\d+) (\d+) (\d+) (\d+)/) {
	my ($user, $nice, $sys, $idle) = ($1, $2, $3, $4);
        $total_cpu_stats->{user} = $user;
        $total_cpu_stats->{nice} = $nice;
        $total_cpu_stats->{sys} = $sys;
        $total_cpu_stats->{idle} = $idle;
        $total_cpu_stats->{total} = add_big_ints([$user, $nice, $sys, $idle]);
        $total_cpu_stats->{frequency} = 100;
    } else {
        die sprintf "linux_get_cpu_usage: Expected cpu at beginning of line, but found the following line instead:\n%s", $line;
    }

    my $i = 0;
    while ($line = <F>) {
        #printf STDERR "\$i=%s \$line='%s'\n", $i, $line;
        if ($line =~ /^\s*cpu(\d+) (\d+) (\d+) (\d+) (\d+)/) {
            my ($cpu, $user, $nice, $sys, $idle) = ($1, $2, $3, $4, $5);
            my $one_cpu_stats = {};
            if ($i != $cpu) {
               die sprintf "linux_get_cpu_usage: Expected cpu%d at beginning of line, but found cpu%d instead.\n", $i, $cpu;
            }
            $one_cpu_stats->{user} = $user;
            $one_cpu_stats->{nice} = $nice;
            $one_cpu_stats->{sys} = $sys;
            $one_cpu_stats->{idle} = $idle;
	    $one_cpu_stats->{total} = add_big_ints([$user, $nice, $sys, $idle]);
            $one_cpu_stats->{frequency} = 100;
            $per_cpu_stats->[$i] = $one_cpu_stats;
            $i++;
        } else {
            last;
        }
    }
    close(F);
    return ($per_cpu_stats, $total_cpu_stats);
}


# Take a list of strings containing integers as an argument, where the
# strings can contain arbitrarily large integers.  Add them and return
# the sum as a string.

sub add_big_ints {
    my $nums = shift;

    my $str = shift @{$nums};
    my $bigint_sum = Math::BigInt->new($str);
    foreach $str (@{$nums}) {
	my $x = Math::BigInt->new($str);
	$bigint_sum->badd($x);
    }
    return sprintf "%s", $bigint_sum;
}


sub cpu_busy_percent {
    my $idle1 = shift;
    my $idle2 = shift;
    my $total1 = shift;
    my $total2 = shift;

    my $total1_bi = Math::BigInt->new($total1);
    my $total_bi = Math::BigInt->new($total2);
    $total_bi->bsub($total1_bi);
    if ($total_bi->is_zero()) {
	return "0";
    } else {
	my $idle1_bi = Math::BigInt->new($idle1);
	my $idle_bi = Math::BigInt->new($idle2);
	$idle_bi->bsub($idle1_bi);

	# Now calculate 100 - ((100 * $idle_bi) / $total_bi), rounded
	# to nearest whole number.  To do that, we will calculate
	# ((1000 * ($total_bi - $idle_bi)) / $total_bi), then do the
	# rounding manually ourselves.  I'm sure that Perl's
	# Math::BigInt package provides a more succinct way of doing
	# this, but this will give the correct answer.
        my $busy_bi = $idle_bi->copy();
        $busy_bi->bsub($total_bi);
        $busy_bi->bneg();
	$busy_bi->bmul(1000);
	$busy_bi->bdiv($total_bi);

	my $last_digit_bi = $busy_bi->copy();
	$last_digit_bi->bmod(10);
	my $cpu_busy_percent_bi = $busy_bi->copy();
	$cpu_busy_percent_bi->bdiv(10);
	if ($last_digit_bi->bcmp(5) >= 0) {   # i.e. ($last_digit_bi >= 5)
	    $cpu_busy_percent_bi->binc();
	}
	return sprintf "%s", $cpu_busy_percent_bi;
    }
}


sub cpu_busy_percent_without_bigints {
    my $idle1 = shift;
    my $idle2 = shift;
    my $total1 = shift;
    my $total2 = shift;

    my $total = ($total2 - $total1);
    my $cpu_busy_percent = 0;
    if ($total != 0) {
	my $idle = ($idle2 - $idle1);
	$cpu_busy_percent = sprintf "%d", (100.0 * (1.0 - (1.0 * $idle) / $total)) + 0.5;
    }
    return sprintf "%d", $cpu_busy_percent;
}


sub time_cmd_location {
    my $os = shift;
    my $install_dir = shift;

    my $timemem_cmd;

    if ($os eq 'Cygwin') {
        # TBD: The default path to timemem should be based on full
        # path name, and where this script itself is installed.

	# TBD: I need some way to convert the Cygwin path to a
	# DOS/Windows path here.  It is not as simple as replacing all
	# / characters with \, because the command will be run from a
	# BAT file, and the full path name must start from the Windows
	# root directory, which is not the same as the Cygwin root
	# directory for files inside of C:\cygwin.  Perhaps parsing
	# the output of the 'mount' command on Cygwin and extracting
	# out the necessary part will work.
	$timemem_cmd = "..\\bin\\timemem";
    } elsif ($os eq 'Darwin') {
	$timemem_cmd = $install_dir . "/timemem-darwin";
    } elsif ($os eq 'GNU/Linux') {
	$timemem_cmd = "/usr/bin/time -v";
    } else {
	printf STDERR "Unknown OS string '%s'.  Only 'Cygwin', 'Darwin', and 'GNU/Linux' are supported.  Aborting.", $os;
	exit 1;
    }
    return $timemem_cmd;
}
