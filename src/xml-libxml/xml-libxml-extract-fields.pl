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

foreach my $measurement ($root->findnodes('Measurement')) {
    my $benchmark_name = $measurement->findvalue('benchmark_name');
    my $user_cpu_time_sec = $measurement->findvalue('user_cpu_time_sec');
    my $maximum_resident_set_size_kibibytes = $measurement->findvalue('maximum_resident_set_size_kibibytes');
    printf "\n";
    printf "    benchmark_name_found_by_xpath='%s'\n", $benchmark_name;
    printf "    user_cpu_time_sec='%s'\n", $user_cpu_time_sec;
    printf "    maximum_resident_set_size_kibibytes='%s'\n", $maximum_resident_set_size_kibibytes;
}

exit 0;
