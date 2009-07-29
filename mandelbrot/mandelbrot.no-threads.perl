# The Computer Language Benchmarks Game
# http://shootout.alioth.debian.org/
# implemented by Greg Buchholz
# streamlined by Kalev Soikonen
# parallelised by Philip Boulain
# modified by Jerry D. Hedden
use warnings; use strict;

use constant ITER     => 50;
use constant LIMITSQR => 2.0 ** 2;
use constant MAXPIXEL => 524288; # Maximum pixel buffer per thread

my ($w, $h);
$w = $h = shift || 80;

# Generate pixel data for a single dot
sub dot($$) {
   my ($Zr, $Zi, $Tr, $Ti) = (0.0,0.0,0.0,0.0);
   my $i = ITER;
   my $Cr = 2 * $_[0] / $w - 1.5;
   my $Ci = 2 * $_[1] / $h - 1.0;
   (
      $Zi = 2.0 * $Zr * $Zi + $Ci,
      $Zr = $Tr - $Ti + $Cr,
      $Ti = $Zi * $Zi,
      $Tr = $Zr * $Zr
   ) until ($Tr + $Ti > LIMITSQR || !$i--);
   return ($i == -1);
}

# Generate pixel data for range of lines, inclusive
sub lines($$) {
   map { my $y = $_;
      pack 'B*', pack 'C*', map dot($_, $y), 0..$w-1;
   } $_[0]..$_[1]
}

$| = 1;
print "P4\n$w $h\n";
my $y = 0;
for $y (0..$h-1) {
    print lines($y, $y);
}
