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
my $mlist = $tree->getDocumentElement;

# Create new measurement and append it to the end.
my $measurement = $tree->createElement('Measurement');
$mlist->appendChild($measurement);

# Fill in the new measurement's fields
my $tag_text_pairs = [];
push @{$tag_text_pairs}, 'benchmark_name', 'nbody';
push @{$tag_text_pairs}, 'language_implementation', 'clj-1.2';
push @{$tag_text_pairs}, 'source_file_name', 'nbody.clj-12.clj';
push @{$tag_text_pairs}, 'user_cpu_time_sec', 43;
push @{$tag_text_pairs}, 'system_cpu_time_sec', 1.1;
push @{$tag_text_pairs}, 'maximum_resident_set_size_kibibytes', 37101;
add_children_to_elem($tree, $measurement, $tag_text_pairs);

# Print out modified XML file

# TBD: Why doesn't using an arg of 1 or 2 cause the whole output file
# to be indented?  It only indents the original part of the file, not
# the new part.  Strange.

print $tree->toString(2);

exit 0;


sub add_children_to_elem {
    my $doc = shift;
    my $elem = shift;
    my $tag_text_pairs = shift;

    my $n = $#{$tag_text_pairs};
    my $i;
    for ($i = 0; $i < $n; $i += 2) {
	my $tag_str = $tag_text_pairs->[$i];
	my $text_str = $tag_text_pairs->[$i+1];
	my $child = $doc->createElement($tag_str);
	my $text = XML::LibXML::Text->new($text_str);
	$child->appendChild($text);
	$elem->appendChild($child);
    }
}
