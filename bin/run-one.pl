: # use perl -*-Perl-*-
eval 'exec perl -S "$0" ${1+"$@"}'
    if 0;
# -*cperl-*-

######################################################################
# Examples of use:

######################################################################
# Windows XP SP3 + Cygwin:
######################################################################

# Admin@john-win ~/git/clojure-benchmarks/knuc
# % ../bin/run-one.pl -i input\\medium-input.txt -o output\\medium-clj-1.2-output.txt \\Program\ Files\\Java\\jrmc-4.0.1-1.6.0\\bin\\java -version
# 0.44,0.36,0.09,17080

######################################################################
# Mac OS X 10.5.8 (and probably also 10.6.x)
######################################################################

# andy@andys-mbp /Users/Shared/lang/clojure-benchmarks/knuc
# % ../bin/run-one.pl -i input/medium-input.txt -o output/medium-clj-1.2-output.txt java -version
# 0.18,0.09,0.03,13688

######################################################################
# Ubuntu 10.4 LTS with GNU time 1.7
######################################################################

# andy@andys-mbp ~/sw/git/clojure-benchmarks/knuc
# % ../bin/run-one.pl -i input/medium-input.txt -o output/medium-clj-1.2-output.txt java -version


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


my $debug = 0;

my $full_progname = $0;
my $progname = fileparse($full_progname);


my $os;
open(F,"uname -o|") or die sprintf "Could not run command 'uname -o' to determine operating system.  Aborting.\n";
$os = <F>;
chomp $os;
close(F);
if ($debug) {
    printf STDERR "\$os='%s'\n", $os;
}


sub usage {
    print STDERR "usage: $progname [-h] [ file1 ... ]\n";
    print STDERR "

description here
";
}

my $opts = { };
getopts('hnds:t:i:o:', $opts);

if ($opts->{h}) {
    usage();
    exit(0);
}

if ($opts->{d}) {
    $debug = 1;
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

my $cmd_to_time = join(' ', @ARGV);
if ($debug) {
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
if ($debug) {
    printf STDERR "\$cmd_str='%s'\n", $cmd_str;
}

my $cmd_to_run;
if ($os eq 'Cygwin') {
    # On Cygwin, put the command into a BAT file for execution.
    #my $tempdir = tempdir( CLEANUP => 1 );
    my $tempdir = tempdir( CLEANUP => 0 );
    my ($fh, $batchfile) = tempfile( DIR => $tempdir, SUFFIX => '.bat' );
    if ($debug) {
	printf STDERR "\$tempdir='%s'\n", $tempdir;
    }
    printf $fh "%s\n", $cmd_str;
    close $fh;
    chmod 0755, $batchfile;
    $cmd_to_run = $batchfile;
} else {
    $cmd_to_run = $cmd_str;
}
if ($debug) {
    printf STDERR "\$cmd_to_run='%s'\n", $cmd_to_run;
}


my ($elapsed_sec, $user_sec, $sys_sec, $max_rss_kbytes);
# I want to run the program and read its stderr output.  I'll assume
# that stdout is already being saved to a file, and ignore anything
# that goes to stdout.

# This is a slight modification of some sample code I found at:
# http://perldoc.perl.org/perlfaq8.html#How-can-I-capture-STDERR-from-an-external-command?

local *CATCHERR = IO::File->new_tmpfile;
my $pid = open3(gensym, \*CATCHOUT, ">&CATCHERR", $cmd_to_run);
while (<CATCHOUT>) {
}
waitpid($pid, 0);
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
if ($debug) {
    printf STDERR "\$elapsed_sec='%s'\n", $elapsed_sec;
    printf STDERR "\$user_sec='%s'\n", $user_sec;
    printf STDERR "\$sys_sec='%s'\n", $sys_sec;
    printf STDERR "\$max_rss_kbytes='%s'\n", $max_rss_kbytes;
}
if ($opts->{n}) {
    printf "elapsed_sec,user_sec,sys_sec,max_rss_kbytes\n";
}
printf "%s,%s,%s,%s", $elapsed_sec, $user_sec, $sys_sec, $max_rss_kbytes;
printf "\n";

exit 0;
