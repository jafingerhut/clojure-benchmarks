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

my $file1 = $ARGV[0];
my $parser1 = XML::LibXML->new();
my $tree1 = $parser1->parse_file($file1);
my $root1 = $tree1->getDocumentElement;

my $file2 = $ARGV[1];
my $parser2 = XML::LibXML->new();
my $tree2 = $parser2->parse_file($file2);
my $root2 = $tree2->getDocumentElement;

foreach my $m2 ($root2->findnodes('Measurement')) {
    $root1->appendChild($m2);
}

print $tree1->toString(1);

exit 0;
