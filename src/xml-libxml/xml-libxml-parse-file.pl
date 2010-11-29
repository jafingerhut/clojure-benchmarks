: # use perl -*-Perl-*-
eval 'exec perl -S "$0" ${1+"$@"}'
    if 0;
# -*cperl-*-

use strict;
use Getopt::Std;
use File::Basename;
use XML::LibXML;

# I wrote this code based on examples found here:

# http://www.xml.com/lpt/a/873


#my $debug = 1;

my $full_progname = $0;
my $progname = fileparse($full_progname);

sub usage {
    print STDERR "usage: $progname [-h] [ file1 ... ]\n";
    print STDERR "

description here
";
}

my $opts = { };
getopts('h', $opts);

if ($opts->{h}) {
    usage();
    exit(0);
}

my $file = $ARGV[0];

my $parser = XML::LibXML->new();
my $tree = $parser->parse_file($file);
my $root = $tree->getDocumentElement;
my @measurements = $root->getElementsByTagName('Measurement');

foreach my $measurement (@measurements) {
    my @benchmark_node = $measurement->getElementsByTagName('benchmark_name');
    my $benchmark_name = $benchmark_node[0]->getFirstChild->getData;
    my @user_cpu_time_sec_node = $measurement->getElementsByTagName('user_cpu_time_sec');
    my $user_cpu_time_sec = $user_cpu_time_sec_node[0]->getFirstChild->getData;
    printf "\n";
    printf "    benchmark_name='%s'\n", $benchmark_name;
    printf "    user_cpu_time_sec='%s'\n", $user_cpu_time_sec;
}

exit 0;
