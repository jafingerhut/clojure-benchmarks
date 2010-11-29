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

# Create a new document
my $doc = XML::LibXML::Document->new();

# ... with root element having tag MeasurementList
my $mlist = $doc->createElement('MeasurementList');
$doc->setDocumentElement($mlist);

# ... and that root element has a first child with tag Measurement
my $measurement = $doc->createElement('Measurement');
$mlist->appendChild($measurement);

# Now fill in the fields of that measurement
my $tag_text_pairs = [];
push @{$tag_text_pairs}, 'benchmark_name', 'knucleotide';
push @{$tag_text_pairs}, 'language_implementation', 'clj-1.2';
push @{$tag_text_pairs}, 'source_file_name', 'knucleotide.clj-8b.clj';
push @{$tag_text_pairs}, 'user_cpu_time_sec', 22.78;
push @{$tag_text_pairs}, 'system_cpu_time_sec', 4.07;
push @{$tag_text_pairs}, 'maximum_resident_set_size_kibibytes', 43202;
add_children_to_elem($doc, $measurement, $tag_text_pairs);

# Now add another Measurement child to the root MeasurementList, and
# fill in its fields differently than the first one.
my $measurement = $doc->createElement('Measurement');
$mlist->appendChild($measurement);
my $tag_text_pairs = [];
push @{$tag_text_pairs}, 'benchmark_name', 'revcomp';
push @{$tag_text_pairs}, 'language_implementation', 'clj-1.2';
push @{$tag_text_pairs}, 'source_file_name', 'revcomp.clj-7.clj';
push @{$tag_text_pairs}, 'user_cpu_time_sec', 9.87;
push @{$tag_text_pairs}, 'system_cpu_time_sec', 0.1;
push @{$tag_text_pairs}, 'maximum_resident_set_size_kibibytes', 123;
add_children_to_elem($doc, $measurement, $tag_text_pairs);


# and finally print out the XML file.
print $doc->toString(1);

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
